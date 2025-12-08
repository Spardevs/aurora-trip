package br.com.ticpass.pos.domain.nfc.usecase

import br.com.ticpass.pos.presentation.nfc.states.NFCUiEvent
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.presentation.nfc.states.NFCSideEffect
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import javax.inject.Inject

/**
 * Use case for handling queue management operations
 */
class QueueManagementUseCase @Inject constructor() {
    
    /**
     * Start processing the nfc queue
     */
    fun startProcessing(
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        emitUiEvent: (NFCUiEvent) -> Unit
    ): NFCSideEffect {
        emitUiEvent(NFCUiEvent.ShowToast("Starting nfc processing"))
        return NFCSideEffect.StartProcessingQueue { nfcQueue.startProcessing() }
    }
    
    /**
     * Enqueue a typed NFC item with operation-specific data
     */
    fun enqueueTypedNFC(
        nfcItem: NFCQueueItem,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        emitUiEvent: (NFCUiEvent) -> Unit
    ): NFCSideEffect {
        val operationType = when (nfcItem) {
            is NFCQueueItem.CustomerAuthOperation -> "Customer Auth"
            is NFCQueueItem.TagFormatOperation -> "Tag Format"
            is NFCQueueItem.CustomerSetupOperation -> "Customer Setup"
            is NFCQueueItem.CartReadOperation -> "Cart Read"
            is NFCQueueItem.CartUpdateOperation -> "Cart Update"
        }
        emitUiEvent(NFCUiEvent.ShowToast("$operationType NFC operation added to queue"))
        return NFCSideEffect.EnqueueNFCItem { nfcQueue.enqueue(nfcItem) }
    }
    
    /**
     * Cancel a specific nfc
     */
    fun cancelNFC(
        nfcId: String,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        emitUiEvent: (NFCUiEvent) -> Unit
    ): NFCSideEffect {
        return NFCSideEffect.RemoveNFCItem {
            val item = nfcQueue.queueState.value.find { it.id == nfcId }
            if (item != null) {
                nfcQueue.remove(item)
                emitUiEvent(NFCUiEvent.ShowToast("NFC cancelled"))
            }
        }
    }
    
    /**
     * Cancel all nfcs
     */
    fun clearQueue(
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        emitUiEvent: (NFCUiEvent) -> Unit
    ): NFCSideEffect {
        emitUiEvent(NFCUiEvent.ShowToast("All nfcs cancelled"))
        return NFCSideEffect.ClearNFCQueue { nfcQueue.clearQueue() }
    }

    fun abortCurrentNFC(
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        emitUiEvent: (NFCUiEvent) -> Unit
    ): NFCSideEffect {
        emitUiEvent(NFCUiEvent.ShowToast("Aborting current nfc"))
        return NFCSideEffect.AbortCurrentNFC { nfcQueue.abort() }
    }
}
