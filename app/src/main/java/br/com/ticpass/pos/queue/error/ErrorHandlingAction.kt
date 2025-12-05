package br.com.ticpass.pos.queue.error

/**
 * Error Handling PaymentProcessingAction
 * Defines the possible actions that can be taken when a processor encounters an error
 */
enum class ErrorHandlingAction {
    /**
     * Retry the same processor immediately without moving the item
     */
    RETRY,

    /**
     * Move the item to the end of the queue for later retry
     */
    SKIP,

    /**
     * Skip this processor but keep the item in queue
     */
    ABORT,

    /**
     * Cancel the entire queue processing
     */
    ABORT_ALL
}
