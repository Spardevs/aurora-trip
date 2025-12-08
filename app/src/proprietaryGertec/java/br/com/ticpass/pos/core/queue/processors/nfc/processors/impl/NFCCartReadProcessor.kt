package br.com.ticpass.pos.core.queue.processors.nfc.processors.impl

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.NFCError
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import javax.inject.Inject

/**
 * ProprietaryGertec NFC Cart Read Processor (NO-OP Stub)
 * Cart read functionality is not supported on ProprietaryGertec devices.
 */
class NFCCartReadProcessor @Inject constructor(
    nfcOperations: NFCOperations
) : NFCProcessorBase(nfcOperations) {

    override suspend fun process(item: NFCQueueItem.CartReadOperation): ProcessingResult {
        return NFCError(ProcessingErrorEvent.FEATURE_UNAVAILABLE)
    }

    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        // NO-OP: Feature not supported
        return true
    }
}
