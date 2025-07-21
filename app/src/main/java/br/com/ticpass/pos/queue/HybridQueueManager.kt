package br.com.ticpass.pos.queue

import android.util.Log
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
    private val queueConfirmationMode: QueueConfirmationMode = QueueConfirmationMode.AUTO,
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
                val existingItems = storage.getAllByStatus("pending")
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
    
    // Remove item from queue
    suspend fun remove(item: T) {
        inMemoryQueue.removeAll { it.id == item.id }
        _queueState.value = inMemoryQueue.toList()
        
        // Remove from pending persistence if it's there
        pendingPersistence.removeAll { it.id == item.id }
        
        // Remove from storage if using persistence
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                storage.remove(item)
            }
        }
    }
    
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
                val nextItem = inMemoryQueue.first()
                
                // Check if we need to confirm before processing the next item
                if (inMemoryQueue.size > 1 && queueConfirmationMode == QueueConfirmationMode.CONFIRMATION) {
                    val nextItemIndex = inMemoryQueue.indexOf(nextItem)
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
                    
                    // If the user chose to skip, remove the item and continue
                    if (response.isCanceled || response.value == false) {
                        // Skip this item
                        inMemoryQueue.removeAt(0)
                        _queueState.value = inMemoryQueue.toList()
                        continue
                    }
                }
                
                try {
                    // Update processing state
                    _processingState.value = ProcessingState.ItemProcessing(nextItem)
                    
                    // Update item status
                    val processingItem = updateItemStatus(nextItem, "processing")

                    Log.d("HybridQueueManager", "$processingItem")
                    // Process the item
                    val result = processor.process(processingItem)
                    
                    when (result) {
                        is ProcessingResult.Success -> {
                            // Remove from queue and mark as completed
                            inMemoryQueue.removeAt(0)
                            _queueState.value = inMemoryQueue.toList()
                            
                            // Remove from pending persistence
                            pendingPersistence.removeAll { it.id == processingItem.id }
                            
                            // Update storage if using persistence
                            if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                scope.launch {
                                    storage.updateStatus(processingItem, "completed")
                                }
                            }
                            
                            _processingState.value = ProcessingState.ItemDone(processingItem)
                        }
                        
                        is ProcessingResult.Error -> {
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
                                    inMemoryQueue.removeAt(0)
                                    val retryItem = updateItemStatus(nextItem, "pending")
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
                                    // Mark as skipped but don't remove from queue
                                    val skippedItem = updateItemStatus(nextItem, "skipped")
                                    inMemoryQueue[0] = skippedItem
                                    _queueState.value = inMemoryQueue.toList()
                                    
                                    // Call the processor's abort method with the current item
                                    scope.launch {
                                        processor.abort(processingItem)
                                    }
                                    
                                    // Update storage if using persistence
                                    if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                        scope.launch {
                                            storage.updateStatus(skippedItem, "skipped")
                                        }
                                    }
                                    
                                    _processingState.value = ProcessingState.ItemSkipped(skippedItem)
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
                                    inMemoryQueue.removeAt(0)
                                    _queueState.value = inMemoryQueue.toList()
                                    
                                    // Remove from pending persistence
                                    pendingPersistence.removeAll { it.id == processingItem.id }
                                    
                                    // Update storage if using persistence
                                    if (persistenceStrategy != PersistenceStrategy.NEVER) {
                                        scope.launch {
                                            storage.updateStatus(processingItem, "failed")
                                        }
                                    }
                                    
                                    _processingState.value = ProcessingState.ItemFailed(processingItem, result.event)
                                }
                            }
                        }
                        
                        is ProcessingResult.Retry -> {
                            // Move to end of queue for retry
                            inMemoryQueue.removeAt(0)
                            val retryItem = updateItemStatus(nextItem, "pending")
                            inMemoryQueue.add(retryItem)
                            _queueState.value = inMemoryQueue.toList()
                            
                            // Add to pending persistence if needed
                            if (persistenceStrategy == PersistenceStrategy.ON_BACKGROUND) {
                                pendingPersistence.add(retryItem)
                            }
                            
                            _processingState.value = ProcessingState.ItemRetrying(processingItem)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("HybridQueueManager", "Error processing item ${nextItem.id}: ${e.message}")
                    // Handle unexpected errors
                    inMemoryQueue.removeAt(0)
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
                            storage.updateStatus(nextItem, "failed")
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
    private fun updateItemStatus(item: T, status: String): T {
        // This is a bit tricky with generics. In a real implementation,
        // you would need to handle this based on your concrete item types
        // For now, we return the original item as we can't easily create a copy with modified status
        return item
    }
    
    // Clear completed items
    suspend fun clearCompleted() {
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                storage.getAllByStatus("completed").forEach { item ->
                    storage.remove(item)
                }
            }
        }
    }
    
    /**
     * Remove all items from the queue at once
     * This is more efficient than removing items individually
     */
    suspend fun removeAll() {
        // Stop processing if active
        isProcessing = false
        
        // Clear in-memory queue
        inMemoryQueue.clear()
        _queueState.value = emptyList()
        _processingState.value = null
        
        // Clear from storage if using persistence
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                // Remove all items from storage
                val allItems = storage.getAllByStatus("pending") + 
                              storage.getAllByStatus("processing")
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
     * Provide input for a queue-level input request
     * 
     * @param response The response to the queue input request
     */
    suspend fun provideQueueInput(response: QueueInputResponse) {
        val continuation = pendingQueueInputContinuations.remove(response.requestId)
        continuation?.resume(response) ?: Log.w("HybridQueueManager", "No pending input request found for ID: ${response.requestId}")
    }
}
