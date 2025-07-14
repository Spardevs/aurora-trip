package br.com.ticpass.pos.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    // In-memory queue for fast access
    private val inMemoryQueue = mutableListOf<T>()
    private val _queueState = MutableStateFlow<List<T>>(emptyList())
    private val _processingState = MutableStateFlow<ProcessingState<T>?>(null)
    
    // Track items that need persistence (for ON_BACKGROUND strategy)
    private val pendingPersistence = mutableSetOf<T>()
    
    // Public observables
    val queueState: StateFlow<List<T>> = _queueState.asStateFlow()
    val processingState: StateFlow<ProcessingState<T>?> = _processingState.asStateFlow()
    
    // Expose processor events directly
    val processorEvents: SharedFlow<E> = processor.events
    
    private var isProcessing = false
    
    init {
        // Load existing items from storage on init (only if we use persistence)
        if (persistenceStrategy != PersistenceStrategy.NEVER) {
            scope.launch {
                val existingItems = storage.getAllByStatus("pending")
                inMemoryQueue.addAll(existingItems)
                _queueState.value = inMemoryQueue.toList()
                
                if (inMemoryQueue.isNotEmpty()) {
                    startProcessing()
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
        if (!isProcessing) {
            startProcessing()
        }
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
    private fun startProcessing() {
        if (isProcessing) return
        
        scope.launch {
            isProcessing = true
            
            while (inMemoryQueue.isNotEmpty()) {
                val nextItem = inMemoryQueue.first()
                
                try {
                    // Update processing state
                    _processingState.value = ProcessingState.Processing(nextItem)
                    
                    // Update item status
                    val processingItem = updateItemStatus(nextItem, "processing")
                    
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
                            
                            _processingState.value = ProcessingState.Completed(processingItem)
                        }
                        
                        is ProcessingResult.Error -> {
                            // Remove from queue and mark as failed
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
                            
                            _processingState.value = ProcessingState.Failed(processingItem, result.message)
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
                            
                            _processingState.value = ProcessingState.Retrying(processingItem)
                        }
                    }
                    
                    // Small delay to show processing state
                    delay(500)
                    
                } catch (e: Exception) {
                    // Handle unexpected errors
                    inMemoryQueue.removeAt(0)
                    _queueState.value = inMemoryQueue.toList()
                    
                    // Remove from pending persistence
                    pendingPersistence.removeAll { it.id == nextItem.id }
                    
                    _processingState.value = ProcessingState.Failed(nextItem, e.message ?: "Unknown error")
                    
                    // Update storage if using persistence
                    if (persistenceStrategy != PersistenceStrategy.NEVER) {
                        scope.launch {
                            storage.updateStatus(nextItem, "failed")
                        }
                    }
                }
            }
            
            isProcessing = false
            _processingState.value = null
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
}
