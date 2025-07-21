package br.com.ticpass.pos.queue

/**
 * Error Handling Action
 * Defines the possible actions that can be taken when a processor encounters an error
 */
enum class ErrorHandlingAction {
    /**
     * Retry the same processor immediately without moving the item
     */
    RETRY_IMMEDIATELY,

    /**
     * Move the item to the end of the queue for later retry
     */
    RETRY_LATER,

    /**
     * Skip this processor but keep the item in queue
     */
    ABORT_CURRENT,

    /**
     * Cancel the entire queue processing
     */
    ABORT_ALL
}
