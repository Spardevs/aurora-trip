package br.com.ticpass.pos.queue

import br.com.ticpass.pos.queue.payment.SystemPaymentMethod

/**
 * Queue Input Response
 * Represents a response to a queue-level input request
 */
data class QueueInputResponse(
    val requestId: String,
    val value: Any? = null,
    val isCanceled: Boolean = false,
    val modifiedAmount: Int? = null,
    val modifiedMethod: SystemPaymentMethod? = null,
    val modifiedProcessorType: String? = null
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
        
        /**
         * Create a proceed response for CONFIRM_NEXT_PROCESSOR with modified payment details
         */
        fun proceedWithModifiedPayment(
            requestId: String,
            modifiedAmount: Int,
            modifiedMethod: SystemPaymentMethod,
            modifiedProcessorType: String
        ): QueueInputResponse {
            return QueueInputResponse(
                requestId = requestId,
                value = true,
                modifiedAmount = modifiedAmount,
                modifiedMethod = modifiedMethod,
                modifiedProcessorType = modifiedProcessorType
            )
        }

        /**
         * Create a skip response for CONFIRM_NEXT_PROCESSOR
         */
        fun skip(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, false)
        }
        
        // Error handling responses
        
        /**
         * Create an immediate retry response for ERROR_RETRY_OR_SKIP
         * Retries the same processor immediately without moving the item
         */
        fun retryImmediately(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.RETRY_IMMEDIATELY)
        }

        /**
         * Create a deferred retry response for ERROR_RETRY_OR_SKIP
         * Moves the item to the end of the queue for later retry
         */
        fun retryLater(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.RETRY_LATER)
        }

        /**
         * Create an abort current processor response for ERROR_RETRY_OR_SKIP
         * Skips this processor but keeps the item in queue
         */
        fun abortCurrentProcessor(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.ABORT_CURRENT)
        }

        /**
         * Create an abort all processors response for ERROR_RETRY_OR_SKIP
         * Cancels the entire queue processing
         */
        fun abortAllProcessors(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, ErrorHandlingAction.ABORT_ALL)
        }
    }
}
