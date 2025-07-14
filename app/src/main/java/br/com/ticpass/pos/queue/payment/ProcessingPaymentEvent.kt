package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.BaseProcessingEvent

/**
 * Payment-specific events emitted during payment processing
 */
sealed class ProcessingPaymentEvent(override val itemId: String) :
    BaseProcessingEvent {
    /**
     * Emitted when payment processing starts
     */
    data class Started(val paymentId: String, val amount: Double) : ProcessingPaymentEvent(paymentId)
    
    /**
     * Emitted when card is detected/inserted
     */
    data class CardDetected(
        val paymentId: String, 
        val cardType: String,
        val cardNumber: String
    ) : ProcessingPaymentEvent(paymentId)
    
    /**
     * Emitted when PIN entry is required
     */
    data class PinRequested(
        val paymentId: String,
        val attemptsLeft: Int = 3
    ) : ProcessingPaymentEvent(paymentId)
    
    /**
     * Emitted when PIN has been entered
     */
    data class PinEntered(val paymentId: String) : ProcessingPaymentEvent(paymentId)
    
    /**
     * Emitted when signature is required
     */
    data class SignatureRequested(val paymentId: String) : ProcessingPaymentEvent(paymentId)
    
    /**
     * Emitted when payment is completed successfully
     */
    data class Completed(
        val paymentId: String, 
        val amount: Double,
        val transactionId: String
    ) : ProcessingPaymentEvent(paymentId)
    
    /**
     * Emitted when payment fails
     */
    data class Failed(
        val paymentId: String,
        val error: String
    ) : ProcessingPaymentEvent(paymentId)
}
