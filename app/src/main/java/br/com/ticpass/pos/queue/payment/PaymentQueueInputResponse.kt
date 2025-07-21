package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.QueueInputResponse

/**
 * Payment-specific Queue Input Response
 * Extends the base QueueInputResponse class with payment-specific response methods
 */
class PaymentQueueInputResponse private constructor(
    requestId: String,
    value: Any? = null,
    isCanceled: Boolean = false,
    val modifiedAmount: Int? = null,
    val modifiedMethod: SystemPaymentMethod? = null,
    val modifiedProcessorType: String? = null
) : QueueInputResponse(requestId, value, isCanceled) {
    
    companion object {
        /**
         * Create a proceed response for CONFIRM_NEXT_PAYMENT with modified payment details
         */
        fun proceedWithModifiedPayment(
            requestId: String,
            modifiedAmount: Int,
            modifiedMethod: SystemPaymentMethod,
            modifiedProcessorType: String
        ): PaymentQueueInputResponse {
            return PaymentQueueInputResponse(
                requestId = requestId,
                value = true,
                modifiedAmount = modifiedAmount,
                modifiedMethod = modifiedMethod,
                modifiedProcessorType = modifiedProcessorType
            )
        }
        
        /**
         * Create a proceed response for CONFIRM_NEXT_PAYMENT without modifications
         */
        fun proceed(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, true)
        }
        
        /**
         * Create a skip response for CONFIRM_NEXT_PAYMENT
         */
        fun skip(requestId: String): QueueInputResponse {
            return QueueInputResponse(requestId, false)
        }
    }
}
