package br.com.ticpass.pos.queue.processors.payment.processors.impl

import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.PaymentError
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.queue.processors.payment.processors.core.PaymentProcessorBase

/**
 * Gertec SK210 Payment Processor (NO-OP)
 * 
 * This variant does not support payment operations.
 * Always returns unsupported error.
 */
class AcquirerPaymentProcessor : PaymentProcessorBase() {

    private val TAG = this.javaClass.simpleName

    override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
        Log.w(TAG, "NO-OP: Payment processing not supported in proprietary Gertec SK210 variant")
        return PaymentError(ProcessingErrorEvent.GENERIC)
    }

    override suspend fun onAbort(item: PaymentProcessingQueueItem?): Boolean {
        Log.d(TAG, "NO-OP: Abort called for payment")
        return true
    }
}
