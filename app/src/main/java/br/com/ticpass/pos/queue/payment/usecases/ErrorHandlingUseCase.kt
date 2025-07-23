package br.com.ticpass.pos.queue.payment.usecases

import br.com.ticpass.pos.queue.ErrorHandlingAction
import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.QueueInputResponse
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.payment.state.SideEffect
import br.com.ticpass.pos.queue.payment.state.UiEvent
import br.com.ticpass.pos.queue.payment.state.UiState
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
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ): SideEffect {
        val response = when (action) {
            ErrorHandlingAction.RETRY_IMMEDIATELY -> QueueInputResponse.retryImmediately(requestId)
            ErrorHandlingAction.RETRY_LATER -> QueueInputResponse.retryLater(requestId)
            ErrorHandlingAction.ABORT_CURRENT -> QueueInputResponse.abortCurrentProcessor(requestId)
            ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.abortAllProcessors(requestId)
        }
        
        // Update UI state for actions that continue processing
        if (action == ErrorHandlingAction.RETRY_IMMEDIATELY || action == ErrorHandlingAction.RETRY_LATER) {
            updateState(UiState.Processing)
            
            // Emit appropriate UI event
            val message = if (action == ErrorHandlingAction.RETRY_IMMEDIATELY) {
                "Retrying payment immediately"
            } else {
                "Payment moved to the end of the queue"
            }
            emitUiEvent(UiEvent.ShowToast(message))
        } else if (action == ErrorHandlingAction.ABORT_CURRENT) {
            emitUiEvent(UiEvent.ShowToast("Skipped current payment"))
        } else if (action == ErrorHandlingAction.ABORT_ALL) {
            emitUiEvent(UiEvent.ShowToast("Cancelled all payments"))
        }
        
        return SideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
    }
    
    /**
     * Retry a failed payment immediately
     */
    fun retryFailedPaymentImmediately(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ): SideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.RETRY_IMMEDIATELY, paymentQueue, emitUiEvent, updateState)
    }
    
    /**
     * Retry a failed payment later (move to end of queue)
     */
    fun retryFailedPaymentLater(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ): SideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.RETRY_LATER, paymentQueue, emitUiEvent, updateState)
    }
    
    /**
     * Abort the current processor
     */
    fun abortCurrentProcessor(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ): SideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.ABORT_CURRENT, paymentQueue, emitUiEvent, updateState)
    }
    
    /**
     * Abort all processors (cancel entire queue)
     */
    fun abortAllProcessors(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ): SideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.ABORT_ALL, paymentQueue, emitUiEvent, updateState)
    }
}
