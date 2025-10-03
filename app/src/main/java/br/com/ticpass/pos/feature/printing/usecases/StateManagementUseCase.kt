package br.com.ticpass.pos.feature.printing.usecases

import br.com.ticpass.pos.feature.printing.state.PrintingUiEvent
import br.com.ticpass.pos.feature.printing.state.PrintingUiState
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
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
        emitUiEvent: (PrintingUiEvent) -> Unit,
        updateState: (PrintingUiState) -> Unit
    ) {
        when (state) {
            is ProcessingState.ItemProcessing -> {
                updateState(PrintingUiState.Processing)
                // Emit event that processing started for this item
                val item = state.item
                if (item is PrintingQueueItem) {
                    emitUiEvent(PrintingUiEvent.ShowToast("Processing printing ${item.id}"))
                }
            }
            is ProcessingState.ItemDone -> {
                // Emit event that item was completed successfully
                val item = state.item
                if (item is PrintingQueueItem) {
                    emitUiEvent(PrintingUiEvent.PrintingCompleted(item.id))
                }
            }
            is ProcessingState.QueueIdle -> {
                updateState(PrintingUiState.Idle)
            }
            is ProcessingState.ItemFailed -> {
                updateState(PrintingUiState.Error(state.error))

                // Emit event that item failed
                val item = state.item
                if (item is PrintingQueueItem) {
                    emitUiEvent(PrintingUiEvent.PrintingFailed(item.id, state.error))
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
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        updateState: (PrintingUiState) -> Unit
    ) {
        when (request) {
            is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                // Get current item from queue to provide item-specific data to UI
                val currentItem = printingQueue.getCurrentItem()
                updateState(PrintingUiState.ConfirmNextProcessor(
                    requestId = request.id,
                    currentItemIndex = request.currentItemIndex,
                    totalItems = request.totalItems,
                    currentItem = currentItem,
                    timeoutMs = request.timeoutMs
                ))
            }
            is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                updateState(PrintingUiState.ErrorRetryOrSkip(
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
        updateState: (PrintingUiState) -> Unit
    ) {
        when (request) {
            is UserInputRequest.CONFIRM_PRINTER_NETWORK_INFO -> {
                updateState(PrintingUiState.ConfirmPrinterNetworkInfo(
                    requestId = request.id,
                    timeoutMs = request.timeoutMs
                ))
            }
            is UserInputRequest.CONFIRM_PRINTER_PAPER_CUT -> {
                updateState(PrintingUiState.ConfirmPrinterPaperCut(
                    requestId = request.id,
                    timeoutMs = request.timeoutMs
                ))
            }
            else -> {}
        }
    }
}
