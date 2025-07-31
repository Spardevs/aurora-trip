package br.com.ticpass.pos.queue.input

import br.com.ticpass.pos.queue.error.ErrorHandlingAction

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
        fun canceled(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, null, true)
        }

        /**
         * Create a proceed response for CONFIRM_NEXT_PROCESSOR
         */
        fun proceed(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, true)
        }
        
        // Payment-specific responses have been moved to PaymentQueueInputResponse class

        /**
         * Create a skip response for CONFIRM_NEXT_PROCESSOR
         */
        fun skip(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, false)
        }
        
        // Error handling responses
        
        /**
         * Create an immediate retry response for RETRY_IMMEDIATELY
         * Retries the same processor immediately without moving the item
         */
        fun retryImmediately(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.RETRY_IMMEDIATELY)
        }

        /**
         * Create a deferred retry response for RETRY_LATER
         * Moves the item to the end of the queue for later retry
         */
        fun retryLater(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.RETRY_LATER)
        }

        /**
         * Create an abort current processor response for ABORT_CURRENT
         * Abort this processor but keeps the item in queue
         */
        fun abortCurrentProcessor(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.ABORT_CURRENT)
        }

        /**
         * Create an abort all processors response for ABORT_ALL
         * Cancels the entire queue processing
         */
        fun abortAllProcessors(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.ABORT_ALL)
        }
    }
}
