package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Transactionless Processor
 * Simulates payment processing without any actual transaction
 * Useful for testing, demos, or when transaction functionality is not available
 */
class TransactionlessProcessor : PaymentProcessorBase() {
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // Simulate a very simplified flow - just emit started and then completed
        
        // Short delay to simulate some processing
        delay(500)
        
        // Always succeeds regardless of amount or payment method
        val transactionId = "SIMULATED-${UUID.randomUUID().toString().substring(0, 8)}"
        _events.emit(
            ProcessingPaymentEvent.Completed(
                item.id,
                item.amount,
                transactionId
            )
        )
        
        // Always return success
        return ProcessingResult.Success
    }
}
