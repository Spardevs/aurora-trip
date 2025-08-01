package br.com.ticpass.pos.queue.processors.payment.processors.impl

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.processors.payment.processors.core.PaymentProcessorBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Stone Payment Processor
 * Processes payments using the acquirer SDK
 */
class AcquirerPaymentProcessor : PaymentProcessorBase() {

    private val tag = "AcquirerPaymentProcessor"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var _item: ProcessingPaymentQueueItem
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            return ProcessingResult.Success(
                atk = "",
                txId =  ""
            )
        }
        catch (exception: Exception) {
            return ProcessingResult.Error(ProcessingErrorEvent.GENERIC)
        }
        finally {
            cleanupCoroutineScopes()
        }
    }

    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        TODO("Not yet implemented")
    }

    private fun cleanupCoroutineScopes() {
        scope.cancel()
    }

}