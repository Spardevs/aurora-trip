package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Cash Payment Processor
 * Processes cash payments without using acquirer SDK
 */
class CashPaymentProcessor : PaymentProcessorBase() {
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // Simulate cash payment flow
        
        // Cash amount entry
        _events.emit(ProcessingPaymentEvent.START)
        delay(1000)
        
        // Calculate change if needed (simulated)
        val amountTendered = item.amount + (0..5).random()
        val change = amountTendered - item.amount
        
        // We would store this information in a real implementation
        // For example, in a receipt or transaction record
        val cashInfo = mapOf(
            "amountTendered" to amountTendered.toString(),
            "change" to change.toString()
        )
        
        // Custom cash event - not in default ProcessingPaymentEvent, but could be added
        // For now we'll use a generic completed event
        delay(1500)
        
        if (item.amount > 0) {
            // Complete the transaction
            val transactionId = "CASH-${UUID.randomUUID().toString().substring(0, 8)}"
            _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)
            return ProcessingResult.Success(
                atk = "",
                txId = ""
            )
        } else {
            return ProcessingResult.Error(ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT)
        }
    }
}
