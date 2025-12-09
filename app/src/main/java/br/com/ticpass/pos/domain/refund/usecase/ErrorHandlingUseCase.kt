package br.com.ticpass.pos.domain.refund.usecase

import br.com.ticpass.pos.presentation.refund.states.RefundUiEvent
import br.com.ticpass.pos.presentation.refund.states.RefundUiState
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.input.QueueInputResponse
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.presentation.refund.states.RefundSideEffect
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import javax.inject.Inject

/**
 * Use case for handling error scenarios and retry logic
 */
class ErrorHandlingUseCase @Inject constructor() {
    
    /**
     * Handle a failed refund with the specified action
     */
    fun handleFailedRefund(
        requestId: String,
        action: ErrorHandlingAction,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        emitUiEvent: (RefundUiEvent) -> Unit,
        updateState: (RefundUiState) -> Unit
    ): RefundSideEffect {
        val response = when (action) {
            ErrorHandlingAction.RETRY -> QueueInputResponse.onErrorRetry(requestId)
            ErrorHandlingAction.SKIP -> QueueInputResponse.onErrorSkip(requestId)
            ErrorHandlingAction.ABORT -> QueueInputResponse.onErrorAbort(requestId)
            ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.onErrorAbortAll(requestId)
        }
        
        // Update UI state for actions that continue processing
        when (action) {
            ErrorHandlingAction.RETRY, ErrorHandlingAction.SKIP -> {
                updateState(RefundUiState.Processing)

                // Emit appropriate UI event
                val message = if (action == ErrorHandlingAction.RETRY) {
                    "Retrying refund immediately"
                } else {
                    "Refund moved to the end of the queue"
                }
                emitUiEvent(RefundUiEvent.ShowToast(message))
            }
            ErrorHandlingAction.ABORT -> {
                emitUiEvent(RefundUiEvent.ShowToast("Aborted current refund"))
            }
            ErrorHandlingAction.ABORT_ALL -> {
                emitUiEvent(RefundUiEvent.ShowToast("Cancelled all refunds"))
            }
        }
        
        return RefundSideEffect.ProvideQueueInput { refundQueue.provideQueueInput(response) }
    }
}
