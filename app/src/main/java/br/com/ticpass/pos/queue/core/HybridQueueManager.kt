package br.com.ticpass.pos.queue.core

import android.util.Log
import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.input.QueueInputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.models.ProcessingState
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
    
    // Track items that need persistence (for ON_BACKGROUND strategy)
    private val pendingPersistence = mutableSetOf<T>()
    
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
                val existingItems = storage.getAllByStatus(QueueItemStatus.PENDING)
                inMemoryQueue.addAll(existingItems)
                _queueState.value = inMemoryQueue.toList()
                
                if (inMemoryQueue.isNotEmpty()) {
//                    startProcessing()
                }
            }
        }
    }
    
    // Enqueue item
    suspend fun enqueue(item: T) {
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
            PersistenceStrategy.ON_BACKGROUND -> {
                // Add to pending persistence set
                pendingPersistence.add(item)
            }
            PersistenceStrategy.NEVER -> {
                // Do nothing - memory only
            }
        }
        
        // Start processing if not already running
//        if (!isProcessing) {
//            startProcessing()
//        }
    }
    
    // Persist all pending items (call when app goes to background)
    suspend fun persistPendingItems() {
        if (persistenceStrategy == PersistenceStrategy.ON_BACKGROUND && pendingPersistence.isNotEmpty()) {
            scope.launch {
                pendingPersistence.forEach { item ->
                    storage.insert(item)
                }
                pendingPersistence.clear()
            }.join() // Wait for completion
        }
    }
    
    // Check if there are items waiting to be persisted
    val hasPendingPersistence: Boolean
        get() = pendingPersistence.isNotEmpty()
    
    // Get count of items waiting to be persisted
    val pendingPersistenceCount: Int
        get() = pendingPersistence.size
    
    // Get queue size
    val size: Int get() = inMemoryQueue.size
    
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
                    Log.d("HybridQueueManager", "Confirmation mode enabled, showing confirmation dialog")
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
                    val response = suspendCancellableCoroutine<QueueInputResponse> { continuation ->
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
                            inMemoryQueue.removeFirstOrNull()
                            _queueState.value = inMemoryQueue.toList()
                            
                            // Remove from pending persistence
                            pendingPersistence.removeAll { it.id == processingItem.id }
                            
                            // Update storage if using persistence
                            if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                scope.launch {
                                    storage.updateStatus(processingItem, QueueItemStatus.COMPLETED)
                                }
                            }
                            
                            _processingState.value = ProcessingState.ItemDone(processingItem)
                        }
                        
                        is ProcessingResult.Error -> {
                            // First emit the ItemFailed state to ensure it's displayed
                            _processingState.value = ProcessingState.ItemFailed(processingItem, result.event)
                            
                            // Log the error for debugging
                            Log.e("ErrorHandling", "ProcessingResult.Error received in HybridQueueManager: ${result.event}")
                            
                            // Create an error retry/skip request
                            val errorRequest = QueueInputRequest.ERROR_RETRY_OR_SKIP(
                                itemId = processingItem.id,
                                error = result.event
                            )
                            
                            // Emit the error request
                            _queueInputRequests.emit(errorRequest)
                            
                            // Wait for response (this will be resumed when provideQueueInput is called)
                            val response = suspendCancellableCoroutine<QueueInputResponse> { continuation ->
                                // Store the continuation to be resumed later
                                pendingQueueInputContinuations[errorRequest.id] = continuation
                            }
                            
                            // Get the error handling action from the response
                            val action = response.getErrorHandlingAction()
                            
                            when (action) {
                                ErrorHandlingAction.RETRY_IMMEDIATELY -> {
                                    // Retry the same processor immediately without moving the item
                                    _processingState.value = ProcessingState.ItemRetrying(processingItem)
                                    // Continue with the same item (don't remove from queue)
                                    continue
                                }
                                ErrorHandlingAction.RETRY_LATER -> {
                                    // Move to end of queue for later retry
                                    inMemoryQueue.removeFirstOrNull()
                                    val retryItem = updateItemStatus(nextItem, QueueItemStatus.PENDING)
                                    inMemoryQueue.add(retryItem)
                                    _queueState.value = inMemoryQueue.toList()
                                    
                                    // Add to pending persistence if needed
                                    if (persistenceStrategy == PersistenceStrategy.ON_BACKGROUND) {
                                        pendingPersistence.add(retryItem)
                                    }
                                    
                                    _processingState.value = ProcessingState.ItemRetrying(processingItem)
                                }
                                ErrorHandlingAction.ABORT_CURRENT -> {
                                    // Skip this processor but keep the item in queue
                                    // Mark as aborted but don't remove from queue
                                    val abortedItem = updateItemStatus(nextItem, QueueItemStatus.CANCELLED)
                                    inMemoryQueue[0] = abortedItem
                                    _queueState.value = inMemoryQueue.toList()
                                    
                                    // Call the processor's abort method with the current item
                                    scope.launch {
                                        processor.abort(processingItem)
                                    }
                                    
                                    // Update storage if using persistence
                                    if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                        scope.launch {
                                            storage.updateStatus(abortedItem,  QueueItemStatus.CANCELLED)
                                        }
                                    }
                                    
                                    _processingState.value = ProcessingState.ItemSkipped(abortedItem)
                                }
                                ErrorHandlingAction.ABORT_ALL -> {
                                    // Cancel the entire queue processing
                                    isProcessing = false
                                    
                                    // Call the processor's abort method to perform graceful cleanup
                                    scope.launch {
                                        processor.abort(null)
                                    }
                                    
                                    // Remove all items from the queue
                                    val removedItems = removeAll()
                                    
                                    _processingState.value = ProcessingState.QueueCanceled(null)
                                    break
                                }
                                else -> {
                                    // Default: Skip this item (remove from queue and mark as failed)
                                    inMemoryQueue.removeFirstOrNull()
                                    _queueState.value = inMemoryQueue.toList()
                                    
                                    // Remove from pending persistence
                                    pendingPersistence.removeAll { it.id == processingItem.id }
                                    
                                    // Update storage if using persistence
                                    if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                        scope.launch {
                                            storage.updateStatus(processingItem, QueueItemStatus.FAILED)
                                        }
                                    }
                                    
                                    _processingState.value = ProcessingState.ItemFailed(processingItem, result.event)
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("HybridQueueManager", "Error processing item ${nextItem.id}: ${e.message}")
                    // Handle unexpected errors
                    inMemoryQueue.removeFirstOrNull()
                    _queueState.value = inMemoryQueue.toList()
                    
                    // Remove from pending persistence
                    pendingPersistence.removeAll { it.id == nextItem.id }
                    
                    _processingState.value = ProcessingState.ItemFailed(
                        nextItem,
                        ProcessingErrorEvent.GENERIC
                    )
                    
                    // Update storage if using persistence
                    if (persistenceStrategy != PersistenceStrategy.NEVER) {
                        scope.launch {
                            storage.updateStatus(nextItem, QueueItemStatus.FAILED)
                        }
                    }
                }
            }
            
            isProcessing = false
            
            // Check if we've processed all items
            if (inMemoryQueue.isEmpty()) {
                _processingState.value = ProcessingState.QueueDone(null)
            } else {
                _processingState.value = null
            }
        }
    }
    
    // Helper to update item status
    private fun updateItemStatus(item: T, status: QueueItemStatus): T {
        // This is a bit tricky with generics. In a real implementation,
        // you would need to handle this based on your concrete item types
        // For now, we return the original item as we can't easily create a copy with modified status
        return item
    }
    
    // Clear completed items
    suspend fun clearCompleted() {
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                storage.getAllByStatus(QueueItemStatus.COMPLETED).forEach { item ->
                    storage.remove(item)
                }
            }
        }
    }

    // Remove item from queue
    suspend fun remove(item: T) {
        if (isProcessing) processor.abort(null)
        isProcessing = false

        inMemoryQueue.removeAll { it.id == item.id }
        _queueState.value = inMemoryQueue.toList()
        _processingState.value = null

        // Remove from pending persistence if it's there
        pendingPersistence.removeAll { it.id == item.id }

        // Remove from storage if using persistence
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                storage.remove(item)
            }
        }
    }
    
    /**
     * Remove all items from the queue at once
     * This is more efficient than removing items individually
     * Also ensures any active processors are properly aborted
     */
    suspend fun removeAll() {
        if (isProcessing) processor.abort(null)
        isProcessing = false
        
        // Clear in-memory queue
        inMemoryQueue.clear()
        _queueState.value = emptyList()
        _processingState.value = null
        
        // Clear from storage if using persistence
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                // Remove all items from storage
                val allItems = storage.getAllByStatus(QueueItemStatus.PENDING) + 
                              storage.getAllByStatus(QueueItemStatus.PROCESSING)
                allItems.forEach { item ->
                    storage.remove(item)
                }
            }.join()
        }
    }
    
    // Force persist all items (useful for testing or manual persistence)
    suspend fun forcePersist() {
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                // Persist all in-memory items
                inMemoryQueue.forEach { item ->
                    storage.insert(item)
                }
                // Clear pending persistence
                pendingPersistence.clear()
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
            
            // Update persistence if needed
            if (persistenceStrategy == PersistenceStrategy.IMMEDIATE) {
                scope.launch {
                    storage.update(modifiedItem)
                }
            } else if (persistenceStrategy == PersistenceStrategy.ON_BACKGROUND) {
                pendingPersistence.add(modifiedItem)
            }
        }
    }

    /**
     * Provide input for a queue-level input request
     * 
     * @param response The response to the queue input request
     */
    suspend fun provideQueueInput(response: QueueInputResponse) {
        val continuation = pendingQueueInputContinuations.remove(response.requestId)
        if (continuation != null) {
            continuation.resume(response)
        } else {
            Log.w("HybridQueueManager", "No pending input request found for ID: ${response.requestId}")
        }
    }
}
