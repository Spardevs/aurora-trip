package br.com.ticpass.pos.core.queue.input

import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction

/**
 * Queue Input Response
 * Represents a response to a queue-level input request
 */
open class QueueInputResponse(
    val requestId: String,
    val value: Any? = null,
    val isCanceled: Boolean = false
) {
    /**
     * Get the error handling action from the response value
     * Returns null if the value is not an ErrorHandlingAction
     */
    fun getErrorHandlingAction(): ErrorHandlingAction? {
        return when (value) {
            is ErrorHandlingAction -> value
            else -> null
        }
    }
    companion object {
        /**
         * Create a canceled response
         */
        fun cancelled(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, null, true)
        }

        /**
         * Create a proceed response for CONFIRM_NEXT_PROCESSOR
         */
        fun proceed(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, true)
        }

        /**
         * Create a skip response for CONFIRM_NEXT_PROCESSOR
         */
        fun skip(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, false)
        }
        
        // Error handling responses
        
        /**
         * Create an immediate retry response for RETRY
         * Retries the same processor immediately without moving the item
         */
        fun onErrorRetry(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.RETRY)
        }

        /**
         * Create a deferred retry response for SKIP
         * Moves the item to the end of the queue for later retry
         */
        fun onErrorSkip(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.SKIP)
        }

        /**
         * Create an abort current processor response for ABORT
         * Abort this processor but keeps the item in queue
         */
        fun onErrorAbort(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.ABORT)
        }

        /**
         * Create an abort all processors response for ABORT_ALL
         * Cancels the entire queue processing
         */
        fun onErrorAbortAll(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.ABORT_ALL)
        }
    }
}
