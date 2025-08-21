package br.com.ticpass.pos.feature.nfc.usecases

import br.com.ticpass.pos.feature.nfc.state.NFCUiEvent
import br.com.ticpass.pos.feature.nfc.state.NFCUiState
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
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
        emitUiEvent: (NFCUiEvent) -> Unit,
        updateState: (NFCUiState) -> Unit
    ) {
        when (state) {
            is ProcessingState.ItemProcessing -> {
                updateState(NFCUiState.Processing)
                // Emit event that processing started for this item
                val item = state.item
                if (item is NFCQueueItem) {
                    emitUiEvent(NFCUiEvent.ShowToast("Processing nfc ${item.id}"))
                }
            }
            is ProcessingState.ItemDone -> {
                // Emit event that item was completed successfully
                val item = state.item
                if (item is NFCQueueItem) {
                    emitUiEvent(NFCUiEvent.NFCCompleted(item.id))
                }
            }
            is ProcessingState.QueueIdle -> {
                updateState(NFCUiState.Idle)
            }
            is ProcessingState.ItemFailed -> {
                updateState(NFCUiState.Error(state.error))

                // Emit event that item failed
                val item = state.item
                if (item is NFCQueueItem) {
                    emitUiEvent(NFCUiEvent.NFCFailed(item.id, state.error))
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
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        updateState: (NFCUiState) -> Unit
    ) {
        when (request) {
            is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                // Get current item from queue to provide item-specific data to UI
                val currentItem = nfcQueue.getCurrentItem()
                updateState(NFCUiState.ConfirmNextProcessor(
                    requestId = request.id,
                    currentItemIndex = request.currentItemIndex,
                    totalItems = request.totalItems,
                    currentItem = currentItem,
                    timeoutMs = request.timeoutMs
                ))
            }
            is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                updateState(NFCUiState.ErrorRetryOrSkip(
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
        updateState: (NFCUiState) -> Unit
    ) {
        when (request) {
            is UserInputRequest.CONFIRM_NFC_KEYS -> {
                updateState(
                    NFCUiState.ConfirmNFCKeys(
                        requestId = request.id,
                        timeoutMs = request.timeoutMs
                    )
                )
            }
            is UserInputRequest.CONFIRM_NFC_TAG_AUTH -> {
                updateState(
                    NFCUiState.ConfirmNFCTagAuth(
                        requestId = request.id,
                        timeoutMs = request.timeoutMs,
                        pin = request.pin
                    )
                )
            }

            is UserInputRequest.CONFIRM_NFC_TAG_CUSTOMER_DATA -> {
                updateState(
                    NFCUiState.ConfirmNFCCustomerData(
                        requestId = request.id,
                        timeoutMs = request.timeoutMs
                    )
                )
            }

            is UserInputRequest.CONFIRM_NFC_TAG_CUSTOMER_SAVE_PIN -> {
                updateState(
                    NFCUiState.ConfirmNFCCustomerSavePin(
                        requestId = request.id,
                        timeoutMs = request.timeoutMs,
                        pin = request.pin
                    )
                )
            }
            else -> {}
        }
    }
}
