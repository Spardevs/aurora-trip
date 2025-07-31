package br.com.ticpass.pos.queue.models

import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Processing State
 * Represents the current state of a queue or item being processed
 */
sealed class ProcessingState<T : QueueItem> {

    /**
     * All processing was canceled
     */
    class QueueCanceled<T : QueueItem> : ProcessingState<T>()

    /**
     * Queue processing is complete (all items processed)
     */
    class QueueDone<T : QueueItem> : ProcessingState<T>()

    /**
     * The whole queue has been aborted
     */
    class QueueAborted<T : QueueItem> : ProcessingState<T>()

    /**
     * Queue is idle with next item ready to be processed
     */
    class QueueIdle<T : QueueItem> : ProcessingState<T>()
    
    /**
     * An item is currently being processed
     */
    class ItemProcessing<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item has been successfully processed
     */
    class ItemDone<T : QueueItem>(val item: T) : ProcessingState<T>()

    /**
     * An item has been aborted and will not be processed for now
     */
    class ItemAborted<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item has failed processing with an error
     */
    class ItemFailed<T : QueueItem>(val item: T, val error: ProcessingErrorEvent) : ProcessingState<T>()
    
    /**
     * An item is being retried
     */
    class ItemRetrying<T : QueueItem>(val item: T) : ProcessingState<T>()
    
    /**
     * An item was skipped but remains in queue
     */
    data class ItemSkipped<T : QueueItem>(val item: T) : ProcessingState<T>()
}
