package br.com.ticpass.pos.core.queue.core

import android.util.Log
import br.com.ticpass.pos.core.queue.config.PersistenceStrategy
import br.com.ticpass.pos.core.queue.config.ProcessorStartMode
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.input.QueueInputRequest
import br.com.ticpass.pos.core.queue.input.QueueInputResponse
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.models.ProcessingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume

/**
 * Generic Hybrid Queue Manager
 * Core class that manages queue items with optional persistence
 * 
 * @param T The queue item type
 * @param E The event type emitted by the processor
 */
class HybridQueueManager<T : QueueItem, E : BaseProcessingEvent>(
    private val storage: QueueStorage<T>,
    internal val processor: QueueProcessor<T, E>,
    private val persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
    val startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    // In-memory queue for fast access
    private val inMemoryQueue = mutableListOf<T>()
    private val _queueState = MutableStateFlow<List<T>>(emptyList())
    private val _processingState = MutableStateFlow<ProcessingState<T>?>(null)
    
    // Queue-level input requests
    private val _queueInputRequests = MutableSharedFlow<QueueInputRequest>(replay = 1)
    
    // Public observables
    val queueState: StateFlow<List<T>> = _queueState.asStateFlow()
    val processingState: StateFlow<ProcessingState<T>?> = _processingState.asStateFlow()
    
    // Expose processor events directly
    val processorEvents: SharedFlow<E> = processor.events
    
    // Expose queue input requests
    val queueInputRequests: SharedFlow<QueueInputRequest> = _queueInputRequests.asSharedFlow()
    
    private var isProcessing = false
    
    // Store continuations for queue input requests
    private val pendingQueueInputContinuations = mutableMapOf<String, CancellableContinuation<QueueInputResponse>>()
    
    init {
        // Load existing items from storage on init (only if we use persistence)
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                val existingItems = storage.getAllByStatus(listOf(QueueItemStatus.PENDING))
                inMemoryQueue.addAll(existingItems)
                _queueState.value = inMemoryQueue.toList()
            }
        }
    }
    
    fun enqueue(item: T) {
        // Fast: Update in-memory first (immediate UI update)
        inMemoryQueue.add(item)
        inMemoryQueue.sortByDescending { it.priority } // Keep sorted by priority
        _queueState.value = inMemoryQueue.toList()
        
        // Handle persistence based on strategy
        when (persistenceStrategy) {
            PersistenceStrategy.IMMEDIATE -> {
                // Save immediately to database
                scope.launch {
                    storage.insert(item)
                }
            }
            PersistenceStrategy.NEVER -> {
                // Do nothing - memory only
            }
        }
    }

    // Get queue size (all pending and processing items in database)
    val fullSize: Int
        get() = runBlocking {
            if (persistenceStrategy == PersistenceStrategy.NEVER) {
                enqueuedSize
            } else {
                storage.getAllByStatus(
                    listOf(
                        QueueItemStatus.PENDING,
                        QueueItemStatus.PROCESSING,
                        QueueItemStatus.COMPLETED
                    )
                ).size
            }
        }

    // Get in-memory pending queue size
    val enqueuedSize: Int get() = runBlocking {
        storage.getAllByStatus(
            listOf(
                QueueItemStatus.PENDING,
                QueueItemStatus.PROCESSING,
            )
        ).size
    }

    // Get in-memory pending queue size
    val currentIndex: Int get() = runBlocking {
        (fullSize - enqueuedSize) + 1
    }
    
    // Check if queue is empty
    val isEmpty: Boolean get() = inMemoryQueue.isEmpty()
    
    // Start processing queue
    fun startProcessing() {
        if (isProcessing) return

        scope.launch {
            isProcessing = true
            
            while (inMemoryQueue.isNotEmpty()) {
                var nextItem = inMemoryQueue.first()
                
                // Check if we need to confirm before processing the next item
                if (startMode == ProcessorStartMode.CONFIRMATION) {
                    try {
                        val nextItemIndex = inMemoryQueue.indexOf(nextItem)

                        // Use generic confirmation request - let the UI layer interpret item-specific data
                        val confirmRequest = QueueInputRequest.CONFIRM_NEXT_PROCESSOR(
                            currentItemIndex = nextItemIndex,
                            totalItems = inMemoryQueue.size,
                            currentItemId = nextItem.id,
                            nextItemId = if (nextItemIndex < inMemoryQueue.size - 1) inMemoryQueue[nextItemIndex + 1].id else null
                        )

                        // Emit the confirmation request
                        _queueInputRequests.emit(confirmRequest)

                        // Wait for response (this will be resumed when provideQueueInput is called)
                        val response = suspendCancellableCoroutine { continuation ->
                            // Store the continuation to be resumed later
                            pendingQueueInputContinuations[confirmRequest.id] = continuation
                        }
                        nextItem = inMemoryQueue.first()

                        // If the user chose to skip, move the item to the end of the queue and continue
                        if (response.isCanceled || response.value == false) {
                            // Move this item to the end of the queue
                            val skippedItem = inMemoryQueue.removeFirstOrNull()
                            if (skippedItem != null) { inMemoryQueue.add(skippedItem) }
                            _queueState.value = inMemoryQueue.toList()
                            continue
                        }
                    } catch (e: Exception) {
                        Log.e("HybridQueueManager", "Error confirming item ${nextItem.id}: ${e.message}")

                        _processingState.value = ProcessingState.ItemFailed(
                            nextItem,
                            ProcessingErrorEvent.GENERIC
                        )
                    }
                }
                
                try {
                    // Update processing state
                    _processingState.value = ProcessingState.ItemProcessing(nextItem)
                    
                    // Update item status
                    val processingItem = updateItemStatus(nextItem, QueueItemStatus.PROCESSING)
                    // Process the item
                    val result = processor.process(processingItem)

                    when (result) {
                        is ProcessingResult.Success -> {
                            // Remove from queue and mark as completed
                            val doneItem = inMemoryQueue.removeFirstOrNull()
                            _queueState.value = inMemoryQueue.toList()
                            
                            // Update storage if using persistence
                            if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                scope.launch {
                                    storage.updateStatus(processingItem, QueueItemStatus.COMPLETED)
                                }
                            }
                            
                            if(doneItem != null) _processingState.value = ProcessingState.ItemDone(doneItem, result)
                        }
                        
                        is ProcessingResult.Error -> {
                            // First emit the ItemFailed state to ensure it's displayed
                            _processingState.value = ProcessingState.ItemFailed(processingItem, result.event)
                            
                            // Log the error for debugging
                            Log.d("ErrorHandling", "ProcessingResult.Error received in HybridQueueManager: ${result.event}")
                            
                            // Create an error retry/skip request
                            val errorRequest = QueueInputRequest.ERROR_RETRY_OR_SKIP(
                                itemId = processingItem.id,
                                error = result.event
                            )
                            
                            // Emit the error request
                            _queueInputRequests.emit(errorRequest)
                            
                            // Wait for response (this will be resumed when provideQueueInput is called)
                            val response = suspendCancellableCoroutine { continuation ->
                                // Store the continuation to be resumed later
                                pendingQueueInputContinuations[errorRequest.id] = continuation
                            }
                            
                            // Get the error handling action from the response
                            val action = response.getErrorHandlingAction()
                            
                            when (action) {
                                ErrorHandlingAction.RETRY -> {
                                    retry()
                                    continue
                                }
                                ErrorHandlingAction.SKIP -> {
                                    skip()
                                }
                                ErrorHandlingAction.ABORT -> {
                                    abort()
                                }
                                ErrorHandlingAction.ABORT_ALL -> {
                                    abortAll()
                                    break
                                }
                                null -> { /* no-op */ }
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("HybridQueueManager", "Error processing item ${nextItem.id}: ${e.message}")
                    
                    _processingState.value = ProcessingState.ItemFailed(
                        nextItem,
                        ProcessingErrorEvent.GENERIC
                    )
                }
            }
            
            isProcessing = false
            
            // Check if we've processed all items
            if (inMemoryQueue.isEmpty()) {
                _processingState.value = ProcessingState.QueueDone()
            } else {
                _processingState.value = null
            }
        }
    }

    /**
     * Update the status of an item in the queue
     *
     * @param item The item to update
     * @param status The new status to set
     * @return The updated item (or a copy of it)
     */
    private fun updateItemStatus(item: T, status: QueueItemStatus): T {
        item.status = status
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch { storage.update(item) }
        }
        return item
    }

    /**
     * Clear all completed items from the queue
     * This will also remove them from storage if persistence is enabled
     */
    fun clearCompleted() {
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                storage.removeByStatus(listOf(QueueItemStatus.COMPLETED))
            }
        }
    }

    /**
     * Abort the current item processing gracefully
     * This will stop the processor and gracefully abort the current item
     */
    fun abort() {
        isProcessing = false

        val currentItem = getCurrentItem()
        if(currentItem != null) {
            scope.launch { processor.abort(currentItem) }
            _processingState.value = ProcessingState.ItemAborted(currentItem)
        }
    }

    /**
     * Abort all processing.
     * This will stop any active processor.
     */
    private fun abortAll() {
        // Cancel the entire queue processing
        isProcessing = false

        // Call the processor's abort method to perform graceful cleanup
        scope.launch { processor.abort(null) }

        _processingState.value = ProcessingState.QueueAborted()
    }

    /**
     * Retry the current item processing.
     * This will set the processing state to retrying
     * and allow the processor to handle it again.
     */
    private fun retry() {
        val currentItem = getCurrentItem()
        if (currentItem != null) _processingState.value = ProcessingState.ItemRetrying(currentItem)

    }

    /**
     * Retry the current item later.
     * This will move the item to the end of the queue
     * and update its status to PENDING.
     */
    private suspend fun skip() {
        val currentItem = inMemoryQueue.removeFirstOrNull()
        if (currentItem == null) {
            _processingState.value = ProcessingState.QueueIdle()
            return
        }

        processor.abort(currentItem)
        val skipItem = updateItemStatus(currentItem, QueueItemStatus.PENDING)
        inMemoryQueue.add(skipItem)
        _queueState.value = inMemoryQueue.toList()

        _processingState.value = ProcessingState.ItemSkipped(skipItem)
    }

    /**
     * Remove a specific item from the queue.
     * This will also remove it from storage if persistence is enabled.
     *
     * @param item The item to remove
     */
    suspend fun remove(item: T) {
        if (isProcessing) processor.abort(null)
        isProcessing = false

        inMemoryQueue.removeAll { it.id == item.id }
        _queueState.value = inMemoryQueue.toList()
        _processingState.value = null

        // Remove from storage if using persistence
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                storage.remove(item)
            }
        }
    }
    
    /**
     * Clears the entire queue items (which are not done).
     * This is more efficient than removing items individually
     * Also ensures any active processors are properly aborted
     */
    suspend fun clearQueue() {
        abortAll()
        
        // Clear in-memory queue
        inMemoryQueue.clear()
        _queueState.value = emptyList()
        _processingState.value = null
        
        // Clear from storage if using persistence
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                // Remove all items from storage
                val allItems = listOf(
                    QueueItemStatus.PENDING,
                    QueueItemStatus.PROCESSING,
                    QueueItemStatus.FAILED,
                    QueueItemStatus.CANCELLED
                )
                storage.removeByStatus(allItems)
            }.join()
        }
    }
    
    /**
     * Get the current item being processed (first item in queue)
     * 
     * @return The current item or null if queue is empty
     */
    fun getCurrentItem(): T? {
        return inMemoryQueue.firstOrNull()
    }
    
    /**
     * Replace the current item in the queue with a modified version
     * This is used by UseCase layer to modify items before processing
     * 
     * @param modifiedItem The modified item to replace the current one
     */
    fun replaceCurrentItem(modifiedItem: T) {
        if (inMemoryQueue.isNotEmpty()) {
            inMemoryQueue[0] = modifiedItem
            _queueState.value = inMemoryQueue.toList()

            when (persistenceStrategy) {
                PersistenceStrategy.IMMEDIATE -> {
                    scope.launch {
                        storage.update(modifiedItem)
                    }
                }
                PersistenceStrategy.NEVER -> {
                    // Do nothing, memory only
                }
            }
        }
    }

    /**
     * Provide input for a queue-level input request
     * 
     * @param response The response to the queue input request
     */
    fun provideQueueInput(response: QueueInputResponse) {
        val continuation = pendingQueueInputContinuations.remove(response.requestId)
        if (continuation != null) {
            continuation.resume(response)
        } else {
            Log.w("HybridQueueManager", "No pending input request found for ID: ${response.requestId}")
        }
    }
}
