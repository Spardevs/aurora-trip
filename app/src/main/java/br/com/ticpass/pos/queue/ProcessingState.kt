package br.com.ticpass.pos.queue

/**
 * Processing State
 * Represents the current state of a queue or item being processed
 */
sealed class ProcessingState<T : QueueItem> {
    /**
     * Queue is idle with next item ready to be processed
     */
    data class QueueIdle<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item is currently being processed
     */
    data class ItemProcessing<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item has been successfully processed
     */
    data class ItemDone<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item has failed processing with an error
     */
    data class ItemFailed<T : QueueItem>(val item: T, val error: ProcessingErrorEvent) : ProcessingState<T>()
    
    /**
     * An item is being retried
     */
    data class ItemRetrying<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item was skipped but remains in queue
     */
    data class ItemSkipped<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * All processing was canceled
     */
    data class QueueCanceled<T : QueueItem>(val item: T?) : ProcessingState<T>()
    
    /**
     * Queue processing is complete (all items processed)
     */
    data class QueueDone<T : QueueItem>(val item: T?) : ProcessingState<T>()
}
