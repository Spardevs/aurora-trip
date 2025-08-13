package br.com.ticpass.pos.feature.refund.usecases

import br.com.ticpass.pos.feature.refund.state.RefundUiEvent
import br.com.ticpass.pos.feature.refund.state.RefundUiState
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueItem
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
        emitUiEvent: (RefundUiEvent) -> Unit,
        updateState: (RefundUiState) -> Unit
    ) {
        when (state) {
            is ProcessingState.ItemProcessing -> {
                updateState(RefundUiState.Processing)
                // Emit event that processing started for this item
                val item = state.item
                if (item is RefundQueueItem) {
                    emitUiEvent(RefundUiEvent.ShowToast("Processing refund ${item.id}"))
                }
            }
            is ProcessingState.ItemDone -> {
                // Emit event that item was completed successfully
                val item = state.item
                if (item is RefundQueueItem) {
                    emitUiEvent(RefundUiEvent.RefundCompleted(item.id))
                }
            }
            is ProcessingState.QueueIdle -> {
                updateState(RefundUiState.Idle)
            }
            is ProcessingState.ItemFailed -> {
                updateState(RefundUiState.Error(state.error))

                // Emit event that item failed
                val item = state.item
                if (item is RefundQueueItem) {
                    emitUiEvent(RefundUiEvent.RefundFailed(item.id, state.error))
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
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        updateState: (RefundUiState) -> Unit
    ) {
        when (request) {
            is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                // Get current item from queue to provide item-specific data to UI
                val currentItem = refundQueue.getCurrentItem()
                updateState(RefundUiState.ConfirmNextProcessor(
                    requestId = request.id,
                    currentItemIndex = request.currentItemIndex,
                    totalItems = request.totalItems,
                    currentItem = currentItem,
                    timeoutMs = request.timeoutMs
                ))
            }
            is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                updateState(RefundUiState.ErrorRetryOrSkip(
                    requestId = request.id,
                    error = request.error,
                    timeoutMs = request.timeoutMs
                ))
            }
        }
    }
    
    /**
     * Handle processor input requests
     */
    fun handleProcessorInputRequest(
        request: UserInputRequest,
        updateState: (RefundUiState) -> Unit
    ) {
        when (request) {
            is UserInputRequest.CONFIRM_PRINTER_NETWORK_INFO -> {
                updateState(RefundUiState.ConfirmPrinterNetworkInfo(
                    requestId = request.id,
                    timeoutMs = request.timeoutMs
                ))
            }
            else -> {}
        }
    }
}
