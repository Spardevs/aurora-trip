package br.com.ticpass.pos.queue.processors.refund.processors.impl

import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.RefundError
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.queue.processors.refund.processors.core.RefundProcessorBase
import javax.inject.Inject

/**
 * Gertec Refund Processor (NO-OP)
 * 
 * This variant does not support refund operations.
 * Always returns unsupported error.
 */
class AcquirerRefundProcessor @Inject constructor() : RefundProcessorBase() {

    private val TAG = this.javaClass.simpleName

    override suspend fun processRefund(item: RefundQueueItem): ProcessingResult {
        Log.w(TAG, "NO-OP: Refund processing not supported in proprietary Gertec variant")
        return RefundError(ProcessingErrorEvent.GENERIC)
    }

    override suspend fun onAbort(item: RefundQueueItem?): Boolean {
        Log.d(TAG, "NO-OP: Abort called for refund")
        return true
    }
}
