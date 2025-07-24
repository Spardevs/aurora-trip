package br.com.ticpass.pos.queue

import br.com.ticpass.pos.queue.payment.SystemPaymentMethod
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType
import java.util.UUID

/**
 * Payment-specific Queue Input Requests
 * Extends the base QueueInputRequest class with payment-specific request types
 */
sealed class PaymentQueueInputRequest : QueueInputRequest() {
    
    /**
     * Request for confirmation before proceeding to the next payment
     * Payment-specific version with payment details
     */
    data class CONFIRM_NEXT_PAYMENT(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long? = 10_000L, // 60 seconds default timeout
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItemId: String,
        val nextItemId: String?,
        val currentAmount: Int,
        val currentMethod: SystemPaymentMethod,
        val currentProcessorType: PaymentProcessorType
    ) : PaymentQueueInputRequest()
}
