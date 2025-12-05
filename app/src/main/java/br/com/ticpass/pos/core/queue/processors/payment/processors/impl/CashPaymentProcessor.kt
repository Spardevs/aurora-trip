package br.com.ticpass.pos.core.queue.processors.payment.processors.impl

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.PaymentError
import br.com.ticpass.pos.core.queue.models.PaymentSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.payment.exceptions.PaymentProcessingException
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.core.queue.processors.payment.processors.core.PaymentProcessorBase
import javax.inject.Inject

/**
 * Cash Payment Processor
 * Processes cash payments without using acquirer SDK.
 */
class CashPaymentProcessor @Inject constructor() : PaymentProcessorBase() {
    
    // Track if processor is currently being aborted
    private val isAborting = AtomicBoolean(false)
    
    override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
        try {
            withContext(Dispatchers.IO) { delay(1500) }

            val transactionId = "CASH-${UUID.randomUUID().toString().substring(0, 8)}"
            val hasLowAmount = item.amount <= 1000

            if(hasLowAmount) return PaymentError(
                ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
            )
            
            withContext(Dispatchers.IO) { delay(1000) }

            _events.emit(PaymentProcessingEvent.TRANSACTION_DONE)

            // Use withContext to ensure we're on a background thread
            withContext(Dispatchers.IO) { delay(1500) }

            return PaymentSuccess(
                atk = "",
                txId = transactionId
            )
        }
        catch (e: PaymentProcessingException) {
            return PaymentError(e.error)
        }
        catch (e: Exception) {
            return PaymentError(ProcessingErrorEvent.GENERIC)
        }
    }

    override suspend fun onAbort(item: PaymentProcessingQueueItem?): Boolean {
        return try {
            isAborting.set(true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
