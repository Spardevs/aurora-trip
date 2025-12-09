package br.com.ticpass.pos.domain.printing.usecase

import br.com.ticpass.pos.presentation.printing.states.PrintingUiEvent
import br.com.ticpass.pos.presentation.printing.states.PrintingUiState
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.input.QueueInputResponse
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.presentation.printing.states.PrintingSideEffect
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem
import javax.inject.Inject

/**
 * Use case for handling error scenarios and retry logic
 */
class ErrorHandlingUseCase @Inject constructor() {
    
    /**
     * Handle a failed printing with the specified action
     */
    fun handleFailedPrinting(
        requestId: String,
        action: ErrorHandlingAction,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        emitUiEvent: (PrintingUiEvent) -> Unit,
        updateState: (PrintingUiState) -> Unit
    ): PrintingSideEffect {
        val response = when (action) {
            ErrorHandlingAction.RETRY -> QueueInputResponse.onErrorRetry(requestId)
            ErrorHandlingAction.SKIP -> QueueInputResponse.onErrorSkip(requestId)
            ErrorHandlingAction.ABORT -> QueueInputResponse.onErrorAbort(requestId)
            ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.onErrorAbortAll(requestId)
        }
        
        // Update UI state for actions that continue processing
        when (action) {
            ErrorHandlingAction.RETRY, ErrorHandlingAction.SKIP -> {
                updateState(PrintingUiState.Processing)

                // Emit appropriate UI event
                val message = if (action == ErrorHandlingAction.RETRY) {
                    "Retrying printing immediately"
                } else {
                    "Printing moved to the end of the queue"
                }
                emitUiEvent(PrintingUiEvent.ShowToast(message))
            }
            ErrorHandlingAction.ABORT -> {
                emitUiEvent(PrintingUiEvent.ShowToast("Aborted current printing"))
            }
            ErrorHandlingAction.ABORT_ALL -> {
                emitUiEvent(PrintingUiEvent.ShowToast("Cancelled all printings"))
            }
        }
        
        return PrintingSideEffect.ProvideQueueInput { printingQueue.provideQueueInput(response) }
    }
}
