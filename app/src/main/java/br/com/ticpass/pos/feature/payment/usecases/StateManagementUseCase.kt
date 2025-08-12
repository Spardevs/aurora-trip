package br.com.ticpass.pos.feature.payment.usecases

import android.util.Log
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
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
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
    ) {
        when (state) {
            is ProcessingState.ItemProcessing -> {
                updateState(PaymentProcessingUiState.Processing)
                // Emit event that processing started for this item
                val item = state.item
                if (item is PaymentProcessingQueueItem) {
                    emitUiEvent(PaymentProcessingUiEvent.ShowToast("Processing payment ${item.id}"))
                }
            }
            is ProcessingState.ItemDone -> {
                // Emit event that item was completed successfully
                val item = state.item
                if (item is PaymentProcessingQueueItem) {
                    emitUiEvent(PaymentProcessingUiEvent.PaymentCompleted(item.id, item.amount))
                }
            }
            is ProcessingState.QueueIdle -> {
                updateState(PaymentProcessingUiState.Idle)
            }
            is ProcessingState.ItemFailed -> {
                updateState(PaymentProcessingUiState.Error(state.error))

                // Emit event that item failed
                val item = state.item
                if (item is PaymentProcessingQueueItem) {
                    emitUiEvent(PaymentProcessingUiEvent.PaymentFailed(item.id, state.error))
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
        paymentQueue: HybridQueueManager<PaymentProcessingQueueItem, PaymentProcessingEvent>,
        updateState: (PaymentProcessingUiState) -> Unit
    ) {
        when (request) {
            is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                // Get current item from queue to provide item-specific data to UI
                val currentItem = paymentQueue.getCurrentItem()
                updateState(PaymentProcessingUiState.ConfirmNextProcessor(
                    requestId = request.id,
                    currentItemIndex = request.currentItemIndex,
                    totalItems = request.totalItems,
                    currentItem = currentItem,
                    timeoutMs = request.timeoutMs
                ))
            }
            is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                updateState(PaymentProcessingUiState.ErrorRetryOrSkip(
                    requestId = request.id,
                    error = request.error,
                    timeoutMs = request.timeoutMs
                ))
            }
            else -> {
                Log.d("StateManagement", "Unhandled queue input request: ${request::class.simpleName}")
            }
        }
    }
    
    /**
     * Handle processor input requests
     */
    fun handleProcessorInputRequest(
        request: UserInputRequest,
        updateState: (PaymentProcessingUiState) -> Unit
    ) {
        when (request) {
            is UserInputRequest.CONFIRM_MERCHANT_PIX_KEY -> {
                updateState(PaymentProcessingUiState.ConfirmMerchantPixKey(
                    requestId = request.id,
                    timeoutMs = request.timeoutMs
                ))
            }
            is UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
                updateState(PaymentProcessingUiState.ConfirmCustomerReceiptPrinting(
                    requestId = request.id,
                    timeoutMs = request.timeoutMs
                ))
            }

            is UserInputRequest.MERCHANT_PIX_SCANNING -> {
                updateState(PaymentProcessingUiState.MerchantPixScanning(
                    requestId = request.id,
                    pixCode = request.pixCode,
                ))
            }

            else -> {}
        }
    }
}
