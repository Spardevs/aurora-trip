package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.sdk.AcquirerSdk

/**
 * PagSeguro Payment Processor
 * Processes payments using the acquirer SDK
 */
class AcquirerPaymentProcessor : PaymentProcessorBase() {
    
    // Reference to the AcquirerSdk singleton
    private val acquirerSdk = AcquirerSdk.payment.getInstance()
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // acquirer logic

        return ProcessingResult.Error(
            "Acquirer payment processing not implemented yet"
        )
    }
}
