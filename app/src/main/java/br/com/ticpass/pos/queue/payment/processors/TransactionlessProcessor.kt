package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Transactionless Processor
 * Simulates payment processing without any actual transaction
 * Useful for production POS, testing, demos, or when transaction functionality is not available
 */
class TransactionlessProcessor : PaymentProcessorBase() {

    // Track if processor is currently being aborted
    private val isAborting = AtomicBoolean(false)
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            withContext(Dispatchers.IO) { delay(1500) }

            val transactionId = "TRANSACTIONLESS-${UUID.randomUUID().toString().substring(0, 8)}"
            val hasLowAmount = item.amount <= 1000

            if(hasLowAmount) return ProcessingResult.Error(
                ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
            )

            withContext(Dispatchers.IO) { delay(1000) }

            _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)

            // Use withContext to ensure we're on a background thread
            withContext(Dispatchers.IO) { delay(1500) }

            return ProcessingResult.Success(
                atk = "",
                txId = transactionId
            )
        }
        catch (e: CancellationException) {
            return ProcessingResult.Error(
                ProcessingErrorEvent.OPERATION_CANCELLED
            )
        }
        catch (e: Exception) {
            return ProcessingResult.Error(ProcessingErrorEvent.GENERIC)
        }
    }

    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        return try {
            isAborting.set(true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
