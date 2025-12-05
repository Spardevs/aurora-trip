package br.com.ticpass.pos.core.queue.processors.payment.processors.impl

import android.util.Log
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.PaymentError
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.core.queue.processors.payment.processors.core.PaymentProcessorBase
import javax.inject.Inject

/**
 * Gertec Payment Processor (NO-OP)
 * 
 * This variant does not support payment operations.
 * Always returns unsupported error.
 */
class AcquirerPaymentProcessor @Inject constructor() : PaymentProcessorBase() {

    private val TAG = this.javaClass.simpleName

    override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
        Log.w(TAG, "NO-OP: Payment processing not supported in proprietary Gertec variant")
        return PaymentError(ProcessingErrorEvent.GENERIC)
    }

    override suspend fun onAbort(item: PaymentProcessingQueueItem?): Boolean {
        Log.d(TAG, "NO-OP: Abort called for payment")
        return true
    }
}
