package br.com.ticpass.pos.feature.refund.state

import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.feature.refund.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.feature.refund.usecases.ConfirmationUseCase
import br.com.ticpass.pos.feature.refund.usecases.QueueManagementUseCase
import br.com.ticpass.pos.feature.refund.usecases.StateManagementUseCase
import javax.inject.Inject

/**
 * Reducer class for handling state transitions and side effects in the RefundViewModel
 * Refactored to use use case classes for better maintainability and testability
 */
class RefundReducer @Inject constructor(
    private val queueManagementUseCase: QueueManagementUseCase,
    private val errorHandlingUseCase: ErrorHandlingUseCase,
    private val confirmationUseCase: ConfirmationUseCase,
    private val stateManagementUseCase: StateManagementUseCase
) {
    
    private lateinit var emitUiEvent: (RefundUiEvent) -> Unit
    private lateinit var updateState: (RefundUiState) -> Unit
    
    /**
     * Initialize the reducer with callback functions from the ViewModel
     */
    fun initialize(
        emitUiEvent: (RefundUiEvent) -> Unit,
        updateState: (RefundUiState) -> Unit
    ) {
        this.emitUiEvent = emitUiEvent
        this.updateState = updateState
    }
    /**
     * Reduce an action to a side effect and update the UI state
     * @param action The action to reduce
     * @param refundQueue The refund queue to operate on
     * @return A side effect to execute, or null if no side effect is needed
     */
    fun reduce(
        action: RefundAction,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>
    ): RefundSideEffect? {
        return when (action) {
            // Queue management actions
            is RefundAction.StartProcessing -> {
                queueManagementUseCase.startProcessing(refundQueue, emitUiEvent)
            }
            is RefundAction.EnqueueRefund -> {
                queueManagementUseCase.enqueueRefund(
                    atk = action.atk,
                    txId = action.txId,
                    isQRCode = action.isQRCode,
                    processorType = action.processorType,
                    refundQueue = refundQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is RefundAction.CancelRefund -> {
                queueManagementUseCase.cancelRefund(
                    refundId = action.refundId,
                    refundQueue = refundQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is RefundAction.ClearQueue -> {
                queueManagementUseCase.clearQueue(refundQueue, emitUiEvent)
            }
            is RefundAction.AbortCurrentRefund -> {
                queueManagementUseCase.abortCurrentRefund(refundQueue, emitUiEvent)
            }
            
            // Processor confirmation actions
            is RefundAction.ConfirmProcessor<*> -> {
                confirmationUseCase.confirmProcessor(
                    requestId = action.requestId,
                    queue = refundQueue,
                    modifiedItem = action.modifiedItem as RefundQueueItem,
                    updateState = updateState
                )
            }
            is RefundAction.SkipProcessor -> {
                confirmationUseCase.skipProcessor(
                    requestId = action.requestId,
                    refundQueue = refundQueue
                )
            }
            is RefundAction.SkipProcessorOnError -> {
                confirmationUseCase.skipProcessorOnError(
                    requestId = action.requestId,
                    refundQueue = refundQueue
                )
            }
            
            // Error handling actions
            is RefundAction.HandleFailedRefund -> {
                errorHandlingUseCase.handleFailedRefund(
                    requestId = action.requestId,
                    action = action.action,
                    refundQueue = refundQueue,
                    emitUiEvent = emitUiEvent,
                    updateState = updateState
                )
            }

            is RefundAction.ConfirmPrinterNetworkInfo -> {
                confirmationUseCase.confirmPrinterNetworkInfo(
                    requestId = action.requestId,
                    networkInfo = action.networkInfo,
                    refundQueue = refundQueue,
                    updateState = updateState
                )
            }

            // Internal actions triggered by events
            is RefundAction.ProcessingStateChanged -> {
                action.state?.let { state ->
                    stateManagementUseCase.handleProcessingStateChange(
                        state = state,
                        emitUiEvent = emitUiEvent,
                        updateState = updateState
                    )
                }
                null // No side effect needed
            }
            is RefundAction.QueueInputRequested -> {
                stateManagementUseCase.handleQueueInputRequest(
                    request = action.request,
                    refundQueue = refundQueue,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            is RefundAction.ProcessorInputRequested -> {
                stateManagementUseCase.handleProcessorInputRequest(
                    request = action.request,
                    updateState = updateState
                )
                null // No side effect needed
            }
        }
    }
}
