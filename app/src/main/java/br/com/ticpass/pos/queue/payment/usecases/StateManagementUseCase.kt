package br.com.ticpass.pos.queue.payment.usecases

import br.com.ticpass.pos.queue.PaymentQueueInputRequest
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.QueueInputRequest
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.payment.state.UiEvent
import br.com.ticpass.pos.queue.payment.state.UiState
import javax.inject.Inject

/**
 * Use case for handling state management operations
 */
class StateManagementUseCase @Inject constructor() {
    
    /**
     * Handle processing state changes
     */
    fun handleProcessingStateChange(
        state: ProcessingState<*>,
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ) {
        when (state) {
            is ProcessingState.ItemProcessing -> {
                updateState(UiState.Processing)
                // Emit event that processing started for this item
                val item = state.item
                if (item is ProcessingPaymentQueueItem) {
                    emitUiEvent(UiEvent.ShowToast("Processing payment ${item.id}"))
                }
            }
            is ProcessingState.ItemDone -> {
                // Emit event that item was completed successfully
                val item = state.item
                if (item is ProcessingPaymentQueueItem) {
                    emitUiEvent(UiEvent.PaymentCompleted(item.id, item.amount))
                }
            }
            is ProcessingState.QueueIdle -> {
                updateState(UiState.Idle)
            }
            is ProcessingState.ItemFailed -> {
                android.util.Log.e("ErrorHandling", "ProcessingState.ItemFailed received in StateManagementUseCase: ${state.error}")
                updateState(UiState.Error(state.error))
                android.util.Log.e("ErrorHandling", "UiState.Error set in StateManagementUseCase")
                // Emit event that item failed
                val item = state.item
                if (item is ProcessingPaymentQueueItem) {
                    emitUiEvent(UiEvent.PaymentFailed(item.id, state.error))
                    android.util.Log.e("ErrorHandling", "UiEvent.PaymentFailed emitted")
                }
            }
            else -> { /* No UI state change for other processing states */ }
        }
    }
    
    /**
     * Handle queue input requests
     */
    fun handleQueueInputRequest(
        request: QueueInputRequest,
        updateState: (UiState) -> Unit
    ) {
        when (request) {
            is PaymentQueueInputRequest.CONFIRM_NEXT_PAYMENT -> {
                android.util.Log.d("StateManagement", "CONFIRM_NEXT_PAYMENT request received")
                updateState(UiState.ConfirmNextPaymentProcessor(
                    requestId = request.id,
                    currentItemIndex = request.currentItemIndex,
                    totalItems = request.totalItems,
                    currentAmount = request.currentAmount,
                    currentMethod = request.currentMethod,
                    currentProcessorType = request.currentProcessorType
                ))
            }
            is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                android.util.Log.e("ErrorHandling", "QueueInputRequest.ERROR_RETRY_OR_SKIP received in StateManagementUseCase")
                updateState(UiState.ErrorRetryOrSkip(
                    requestId = request.id,
                    error = request.error
                ))
                android.util.Log.e("ErrorHandling", "UiState.ErrorRetryOrSkip set in StateManagementUseCase")
            }
            else -> {
                android.util.Log.d("StateManagement", "Unhandled queue input request: ${request::class.simpleName}")
            }
        }
    }
}
