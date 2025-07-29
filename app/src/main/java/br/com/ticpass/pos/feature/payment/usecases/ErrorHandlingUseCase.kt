package br.com.ticpass.pos.feature.payment.usecases

import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.QueueInputResponse
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingSideEffect
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
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
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        val response = when (action) {
            ErrorHandlingAction.RETRY_IMMEDIATELY -> QueueInputResponse.retryImmediately(requestId)
            ErrorHandlingAction.RETRY_LATER -> QueueInputResponse.retryLater(requestId)
            ErrorHandlingAction.ABORT_CURRENT -> QueueInputResponse.abortCurrentProcessor(requestId)
            ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.abortAllProcessors(requestId)
        }
        
        // Update UI state for actions that continue processing
        if (action == ErrorHandlingAction.RETRY_IMMEDIATELY || action == ErrorHandlingAction.RETRY_LATER) {
            updateState(PaymentProcessingUiState.Processing)
            
            // Emit appropriate UI event
            val message = if (action == ErrorHandlingAction.RETRY_IMMEDIATELY) {
                "Retrying payment immediately"
            } else {
                "Payment moved to the end of the queue"
            }
            emitUiEvent(PaymentProcessingUiEvent.ShowToast(message))
        } else if (action == ErrorHandlingAction.ABORT_CURRENT) {
            emitUiEvent(PaymentProcessingUiEvent.ShowToast("Skipped current payment"))
        } else if (action == ErrorHandlingAction.ABORT_ALL) {
            emitUiEvent(PaymentProcessingUiEvent.ShowToast("Cancelled all payments"))
        }
        
        return PaymentProcessingSideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
    }
    
    /**
     * Retry a failed payment immediately
     */
    fun retryFailedPaymentImmediately(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.RETRY_IMMEDIATELY, paymentQueue, emitUiEvent, updateState)
    }
    
    /**
     * Retry a failed payment later (move to end of queue)
     */
    fun retryFailedPaymentLater(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.RETRY_LATER, paymentQueue, emitUiEvent, updateState)
    }
    
    /**
     * Abort the current processor
     */
    fun abortCurrentProcessor(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.ABORT_CURRENT, paymentQueue, emitUiEvent, updateState)
    }
    
    /**
     * Abort all processors (cancel entire queue)
     */
    fun abortAllProcessors(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        return handleFailedPayment(requestId, ErrorHandlingAction.ABORT_ALL, paymentQueue, emitUiEvent, updateState)
    }
}
