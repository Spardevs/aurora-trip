package br.com.ticpass.pos.queue.processors.nfc.processors.impl

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.NFCError
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCOperations
import javax.inject.Inject

/**
 * ProprietaryGertec NFC Cart Update Processor (NO-OP Stub)
 * Cart update functionality is not supported on ProprietaryGertec devices.
 */
class NFCCartUpdateProcessor @Inject constructor(
    nfcOperations: NFCOperations
) : NFCProcessorBase(nfcOperations) {

    override suspend fun process(item: NFCQueueItem.CartUpdateOperation): ProcessingResult {
        return NFCError(ProcessingErrorEvent.FEATURE_UNAVAILABLE)
    }

    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        // NO-OP: Feature not supported
        return true
    }
}
