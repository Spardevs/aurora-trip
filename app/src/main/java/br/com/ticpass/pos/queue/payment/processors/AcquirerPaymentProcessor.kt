package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Acquirer Payment Processor
 * Processes payments using the acquirer SDK
 */
class AcquirerPaymentProcessor : PaymentProcessorBase() {
    
    // Reference to the AcquirerSdk singleton
    private val acquirerSdk = AcquirerSdk.payment
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // acquirer logic

        return ProcessingResult.Error(
            "Acquirer payment processing not implemented yet"
        )
    }
}
