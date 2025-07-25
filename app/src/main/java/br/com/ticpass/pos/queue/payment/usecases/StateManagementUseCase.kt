package br.com.ticpass.pos.queue.payment.usecases

import android.util.Log
import br.com.ticpass.pos.queue.InputRequest
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
                    currentProcessorType = request.currentProcessorType,
                    timeoutMs = request.timeoutMs
                ))
            }
            is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                android.util.Log.e("ErrorHandling", "QueueInputRequest.ERROR_RETRY_OR_SKIP received in StateManagementUseCase")
                updateState(UiState.ErrorRetryOrSkip(
                    requestId = request.id,
                    error = request.error,
                    timeoutMs = request.timeoutMs
                ))
                android.util.Log.e("ErrorHandling", "UiState.ErrorRetryOrSkip set in StateManagementUseCase")
            }
            else -> {
                android.util.Log.d("StateManagement", "Unhandled queue input request: ${request::class.simpleName}")
            }
        }
    }
    
    /**
     * Handle processor input requests
     */
    fun handleProcessorInputRequest(
        request: InputRequest,
        updateState: (UiState) -> Unit
    ) {
        when (request) {
            is InputRequest.CONFIRM_MERCHANT_PIX_KEY -> {
                Log.d("StateManagement", "CONFIRM_MERCHANT_PIX_KEY request received")
                updateState(UiState.ConfirmMerchantPixKey(
                    requestId = request.id,
                    pixKey = request.pixKey,
                    timeoutMs = request.timeoutMs
                ))
            }
            is InputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
                Log.d("StateManagement", "CONFIRM_CUSTOMER_RECEIPT_PRINTING request received")
                updateState(UiState.ConfirmCustomerReceiptPrinting(
                    requestId = request.id,
                ))
            }
        }
    }
}
