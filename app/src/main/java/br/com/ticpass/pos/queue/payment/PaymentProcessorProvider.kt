package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.QueueProcessor
import br.com.ticpass.pos.queue.payment.processors.CashPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.AcquirerPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.TransactionlessProcessor

/**
 * Payment Processor Provider
 * Factory class to provide the appropriate payment processor based on payment method
 */
class PaymentProcessorProvider {
    /**
     * Get the appropriate payment processor based on payment method
     *
     * @param paymentMethod The payment method to use (e.g., "acquirer", "cash", "transactionless")
     * @return A payment processor that can handle the specified payment method
     */
    fun getProcessor(paymentMethod: String): QueueProcessor<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
        return when (paymentMethod.lowercase()) {
            "cash" -> CashPaymentProcessor()
            "transactionless" -> TransactionlessProcessor()
            else -> AcquirerPaymentProcessor() // Default processor (uses acquirer SDK)
        }
    }
}
