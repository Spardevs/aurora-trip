package br.com.ticpass.pos.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.NFCError
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.processors.core.NFCProcessorBase

/**
 * Gertec SK210 NFC Customer Auth Processor (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Always returns unsupported error.
 */
class NFCCustomerAuthProcessor : NFCProcessorBase() {

    private val TAG = this.javaClass.simpleName

    override suspend fun process(item: NFCQueueItem.CustomerAuthOperation): ProcessingResult {
        Log.w(TAG, "NO-OP: NFC Customer Auth not supported in proprietary Gertec SK210 variant")
        return NFCError(ProcessingErrorEvent.GENERIC)
    }

    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        TODO("Not yet implemented")
    }
}
