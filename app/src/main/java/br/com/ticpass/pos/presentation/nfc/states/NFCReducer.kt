package br.com.ticpass.pos.presentation.nfc.states

import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.domain.nfc.usecase.ErrorHandlingUseCase
import br.com.ticpass.pos.domain.nfc.usecase.ConfirmationUseCase
import br.com.ticpass.pos.domain.nfc.usecase.QueueManagementUseCase
import br.com.ticpass.pos.domain.nfc.usecase.StateManagementUseCase
import javax.inject.Inject

/**
 * Reducer class for handling state transitions and side effects in the NFCViewModel
 * Refactored to use use case classes for better maintainability and testability
 */
class NFCReducer @Inject constructor(
    private val queueManagementUseCase: QueueManagementUseCase,
    private val errorHandlingUseCase: ErrorHandlingUseCase,
    private val confirmationUseCase: ConfirmationUseCase,
    private val stateManagementUseCase: StateManagementUseCase
) {
    
    private lateinit var emitUiEvent: (NFCUiEvent) -> Unit
    private lateinit var updateState: (NFCUiState) -> Unit
    
    /**
     * Initialize the reducer with callback functions from the ViewModel
     */
    fun initialize(
        emitUiEvent: (NFCUiEvent) -> Unit,
        updateState: (NFCUiState) -> Unit
    ) {
        this.emitUiEvent = emitUiEvent
        this.updateState = updateState
    }
    /**
     * Reduce an action to a side effect and update the UI state
     * @param action The action to reduce
     * @param nfcQueue The nfc queue to operate on
     * @return A side effect to execute, or null if no side effect is needed
     */
    fun reduce(
        action: NFCAction,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>
    ): NFCSideEffect? {
        return when (action) {
            // Queue management actions
            is NFCAction.StartProcessing -> {
                queueManagementUseCase.startProcessing(nfcQueue, emitUiEvent)
            }
            is NFCAction.EnqueueTypedNFC -> {
                queueManagementUseCase.enqueueTypedNFC(
                    nfcItem = action.nfcItem,
                    nfcQueue = nfcQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is NFCAction.CancelNFC -> {
                queueManagementUseCase.cancelNFC(
                    nfcId = action.nfcId,
                    nfcQueue = nfcQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is NFCAction.ClearQueue -> {
                queueManagementUseCase.clearQueue(nfcQueue, emitUiEvent)
            }
            is NFCAction.AbortCurrentNFC -> {
                queueManagementUseCase.abortCurrentNFC(nfcQueue, emitUiEvent)
            }
            
            // Processor confirmation actions
            is NFCAction.ConfirmProcessor<*> -> {
                confirmationUseCase.confirmProcessor(
                    requestId = action.requestId,
                    queue = nfcQueue,
                    modifiedItem = action.modifiedItem as NFCQueueItem,
                    updateState = updateState
                )
            }
            is NFCAction.SkipProcessor -> {
                confirmationUseCase.skipProcessor(
                    requestId = action.requestId,
                    nfcQueue = nfcQueue
                )
            }
            is NFCAction.SkipProcessorOnError -> {
                confirmationUseCase.skipProcessorOnError(
                    requestId = action.requestId,
                    nfcQueue = nfcQueue
                )
            }
            
            // Error handling actions
            is NFCAction.HandleFailedNFC -> {
                errorHandlingUseCase.handleFailedNFC(
                    requestId = action.requestId,
                    action = action.action,
                    nfcQueue = nfcQueue,
                    emitUiEvent = emitUiEvent,
                    updateState = updateState
                )
            }

            // Internal actions triggered by events
            is NFCAction.ProcessingStateChanged -> {
                action.state?.let { state ->
                    stateManagementUseCase.handleProcessingStateChange(
                        state = state,
                        emitUiEvent = emitUiEvent,
                        updateState = updateState
                    )
                }
                null // No side effect needed
            }
            is NFCAction.QueueInputRequested -> {
                stateManagementUseCase.handleQueueInputRequest(
                    request = action.request,
                    nfcQueue = nfcQueue,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            is NFCAction.ProcessorInputRequested -> {
                stateManagementUseCase.handleProcessorInputRequest(
                    request = action.request,
                    updateState = updateState
                )
                null // No side effect needed
            }

            is NFCAction.ConfirmNFCKeys ->  {
                confirmationUseCase.confirmNFCKeys(
                    requestId = action.requestId,
                    keys = action.keys,
                    updateState = updateState,
                    nfcQueue = nfcQueue,
                )
            }

            is NFCAction.ConfirmNFCTagAuth ->  {
                confirmationUseCase.confirmNFCTagAuth(
                    requestId = action.requestId,
                    didAuth = action.didAuth,
                    updateState = updateState,
                    nfcQueue = nfcQueue,
                )
            }

            is NFCAction.ConfirmNFCCustomerData -> {
                confirmationUseCase.confirmNFCustomerData(
                    requestId = action.requestId,
                    data = action.data,
                    updateState = updateState,
                    nfcQueue = nfcQueue,
                )
            }

            is NFCAction.ConfirmNFCCustomerSavePin -> {
                confirmationUseCase.confirmNFCCustomerSavePin(
                    requestId = action.requestId,
                    didSave = action.didSave,
                    updateState = updateState,
                    nfcQueue = nfcQueue,
                )
            }
        }
    }
}
