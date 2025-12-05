package br.com.ticpass.pos.core.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.NFCError
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import javax.inject.Inject

/**
 * Gertec NFC Customer Auth Processor (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Always returns unsupported error.
 */
class NFCCustomerAuthProcessor @Inject constructor(
    nfcOperations: NFCOperations
) : NFCProcessorBase(nfcOperations) {

    private val TAG = this.javaClass.simpleName

    override suspend fun process(item: NFCQueueItem.CustomerAuthOperation): ProcessingResult {
        Log.w(TAG, "NO-OP: NFC Customer Auth not supported in proprietary Gertec variant")
        return NFCError(ProcessingErrorEvent.GENERIC)
    }

    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        return true
    }
}
