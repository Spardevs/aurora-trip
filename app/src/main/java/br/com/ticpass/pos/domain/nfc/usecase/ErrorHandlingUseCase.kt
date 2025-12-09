package br.com.ticpass.pos.domain.nfc.usecase

import br.com.ticpass.pos.presentation.nfc.states.NFCUiEvent
import br.com.ticpass.pos.presentation.nfc.states.NFCUiState
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.input.QueueInputResponse
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.presentation.nfc.states.NFCSideEffect
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import javax.inject.Inject

/**
 * Use case for handling error scenarios and retry logic
 */
class ErrorHandlingUseCase @Inject constructor() {
    
    /**
     * Handle a failed nfc with the specified action
     */
    fun handleFailedNFC(
        requestId: String,
        action: ErrorHandlingAction,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        emitUiEvent: (NFCUiEvent) -> Unit,
        updateState: (NFCUiState) -> Unit
    ): NFCSideEffect {
        val response = when (action) {
            ErrorHandlingAction.RETRY -> QueueInputResponse.onErrorRetry(requestId)
            ErrorHandlingAction.SKIP -> QueueInputResponse.onErrorSkip(requestId)
            ErrorHandlingAction.ABORT -> QueueInputResponse.onErrorAbort(requestId)
            ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.onErrorAbortAll(requestId)
        }
        
        // Update UI state for actions that continue processing
        when (action) {
            ErrorHandlingAction.RETRY, ErrorHandlingAction.SKIP -> {
                updateState(NFCUiState.Processing)

                // Emit appropriate UI event
                val message = if (action == ErrorHandlingAction.RETRY) {
                    "Retrying nfc immediately"
                } else {
                    "NFC moved to the end of the queue"
                }
                emitUiEvent(NFCUiEvent.ShowToast(message))
            }
            ErrorHandlingAction.ABORT -> {
                emitUiEvent(NFCUiEvent.ShowToast("Aborted current nfc"))
            }
            ErrorHandlingAction.ABORT_ALL -> {
                emitUiEvent(NFCUiEvent.ShowToast("Cancelled all nfcs"))
            }
        }
        
        return NFCSideEffect.ProvideQueueInput { nfcQueue.provideQueueInput(response) }
    }
}
