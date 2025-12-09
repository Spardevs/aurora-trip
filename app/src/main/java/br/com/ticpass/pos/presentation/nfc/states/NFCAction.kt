package br.com.ticpass.pos.presentation.nfc.states

import br.com.ticpass.pos.core.nfc.models.NFCTagCustomerDataInput
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.input.UserInputRequest
import br.com.ticpass.pos.core.queue.models.ProcessingState
import br.com.ticpass.pos.core.queue.input.QueueInputRequest
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem

/**
 * Represents an action that can be dispatched to the ViewModel
 * Actions trigger state transitions and side effects
 */
sealed class NFCAction {
    // Queue actions
    object StartProcessing : NFCAction()
    data class EnqueueTypedNFC(val nfcItem: NFCQueueItem) : NFCAction()
    data class CancelNFC(val nfcId: String) : NFCAction()
    object ClearQueue : NFCAction()
    object AbortCurrentNFC : NFCAction()

    // nfc keys actions
    data class ConfirmNFCKeys(
        val requestId: String,
        val keys: Map<NFCTagSectorKeyType, String>
    ) : NFCAction()

    data class ConfirmNFCTagAuth(
        val requestId: String,
        val didAuth: Boolean
    ) : NFCAction()

    data class ConfirmNFCCustomerData(
        val requestId: String,
        val data: NFCTagCustomerDataInput?
    ) : NFCAction()

    data class ConfirmNFCCustomerSavePin(
        val requestId: String,
        val didSave: Boolean
    ) : NFCAction()
    
    // Processor input actions
    data class ConfirmProcessor<T: QueueItem>(val requestId: String, val modifiedItem: T) : NFCAction()

    data class SkipProcessor(val requestId: String) : NFCAction()

    data class SkipProcessorOnError(val requestId: String) : NFCAction()
    
    // Error handling actions
    data class HandleFailedNFC(
        val requestId: String,
        val action: ErrorHandlingAction
    ) : NFCAction()
    
    // Internal actions triggered by events
    data class ProcessingStateChanged(val state: ProcessingState<*>?) : NFCAction()
    data class QueueInputRequested(val request: QueueInputRequest) : NFCAction()
    data class ProcessorInputRequested(val request: UserInputRequest) : NFCAction()
}
