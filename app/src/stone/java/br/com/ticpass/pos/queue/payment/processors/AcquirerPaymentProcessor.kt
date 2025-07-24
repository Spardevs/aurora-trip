package br.com.ticpass.pos.queue.payment.processors

import android.util.Log
import br.com.ticpass.Constants.CONVERSION_FACTOR
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.queue.payment.SystemCustomerReceiptPrinting
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

    private fun cleanupCoroutineScopes() {
        scope.cancel()
    }
    
    /**
     * Stone-specific abort logic
     * Cancels any ongoing payment transaction and cleans up resources
     */
    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        return try {
            true
        } catch (e: Exception) {
            false
        }
    }

}