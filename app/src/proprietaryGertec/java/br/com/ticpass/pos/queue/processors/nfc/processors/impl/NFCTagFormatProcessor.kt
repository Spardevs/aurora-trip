package br.com.ticpass.pos.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.NFCError
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.processors.core.NFCProcessorBase

/**
 * Gertec NFC Tag Format Processor (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Always returns unsupported error.
 */
class NFCTagFormatProcessor : NFCProcessorBase() {

    private val TAG = this.javaClass.simpleName

    override suspend fun process(item: NFCQueueItem.TagFormatOperation): ProcessingResult {
        Log.w(TAG, "NO-OP: NFC Tag Format not supported in proprietary Gertec variant")
        return NFCError(ProcessingErrorEvent.GENERIC)
    }

    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        TODO("Not yet implemented")
    }
}
