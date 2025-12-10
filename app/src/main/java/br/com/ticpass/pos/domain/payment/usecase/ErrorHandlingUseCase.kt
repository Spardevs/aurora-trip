package br.com.ticpass.pos.domain.payment.usecase

import br.com.ticpass.pos.presentation.payment.states.PaymentProcessingUiEvent
import br.com.ticpass.pos.presentation.payment.states.PaymentProcessingUiState
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.input.QueueInputResponse
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.presentation.payment.states.PaymentProcessingSideEffect
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import javax.inject.Inject

/**
 * Use case for handling error scenarios and retry logic
 */
class ErrorHandlingUseCase @Inject constructor() {
    
    /**
     * Handle a failed payment with the specified action
     */
    fun handleFailedPayment(
        requestId: String,
        action: ErrorHandlingAction,
        paymentQueue: HybridQueueManager<PaymentProcessingQueueItem, PaymentProcessingEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        val response = when (action) {
            ErrorHandlingAction.RETRY -> QueueInputResponse.onErrorRetry(requestId)
            ErrorHandlingAction.SKIP -> QueueInputResponse.onErrorSkip(requestId)
            ErrorHandlingAction.ABORT -> QueueInputResponse.onErrorAbort(requestId)
            ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.onErrorAbortAll(requestId)
        }
        
        // Update UI state for actions that continue processing
        when (action) {
            ErrorHandlingAction.RETRY, ErrorHandlingAction.SKIP -> {
                updateState(PaymentProcessingUiState.Processing)

                // Emit appropriate UI event
                val message = if (action == ErrorHandlingAction.RETRY) {
                    "Retrying payment immediately"
                } else {
                    "Payment moved to the end of the queue"
                }
                emitUiEvent(PaymentProcessingUiEvent.ShowToast(message))
            }
            ErrorHandlingAction.ABORT -> {
                emitUiEvent(PaymentProcessingUiEvent.ShowToast("Aborted current payment"))
            }
            ErrorHandlingAction.ABORT_ALL -> {
                emitUiEvent(PaymentProcessingUiEvent.ShowToast("Cancelled all payments"))
            }
        }
        
        return PaymentProcessingSideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
    }
}
