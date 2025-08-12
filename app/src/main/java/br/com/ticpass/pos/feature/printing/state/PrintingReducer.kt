package br.com.ticpass.pos.feature.printing.state

import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.feature.printing.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.feature.printing.usecases.ConfirmationUseCase
import br.com.ticpass.pos.feature.printing.usecases.QueueManagementUseCase
import br.com.ticpass.pos.feature.printing.usecases.StateManagementUseCase
import javax.inject.Inject

/**
 * Reducer class for handling state transitions and side effects in the PrintingViewModel
 * Refactored to use use case classes for better maintainability and testability
 */
class PrintingReducer @Inject constructor(
    private val queueManagementUseCase: QueueManagementUseCase,
    private val errorHandlingUseCase: ErrorHandlingUseCase,
    private val confirmationUseCase: ConfirmationUseCase,
    private val stateManagementUseCase: StateManagementUseCase
) {
    
    private lateinit var emitUiEvent: (PrintingUiEvent) -> Unit
    private lateinit var updateState: (PrintingUiState) -> Unit
    
    /**
     * Initialize the reducer with callback functions from the ViewModel
     */
    fun initialize(
        emitUiEvent: (PrintingUiEvent) -> Unit,
        updateState: (PrintingUiState) -> Unit
    ) {
        this.emitUiEvent = emitUiEvent
        this.updateState = updateState
    }
    /**
     * Reduce an action to a side effect and update the UI state
     * @param action The action to reduce
     * @param printingQueue The printing queue to operate on
     * @return A side effect to execute, or null if no side effect is needed
     */
    fun reduce(
        action: PrintingAction,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>
    ): PrintingSideEffect? {
        return when (action) {
            // Queue management actions
            is PrintingAction.StartProcessing -> {
                queueManagementUseCase.startProcessing(printingQueue, emitUiEvent)
            }
            is PrintingAction.EnqueuePrinting -> {
                queueManagementUseCase.enqueuePrinting(
                    filePath = action.filePath,
                    processorType = action.processorType,
                    printingQueue = printingQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is PrintingAction.CancelPrinting -> {
                queueManagementUseCase.cancelPrinting(
                    printingId = action.printingId,
                    printingQueue = printingQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is PrintingAction.ClearQueue -> {
                queueManagementUseCase.clearQueue(printingQueue, emitUiEvent)
            }
            is PrintingAction.AbortCurrentPrinting -> {
                queueManagementUseCase.abortCurrentPrinting(printingQueue, emitUiEvent)
            }
            
            // Processor confirmation actions
            is PrintingAction.ConfirmProcessor<*> -> {
                confirmationUseCase.confirmProcessor(
                    requestId = action.requestId,
                    queue = printingQueue,
                    modifiedItem = action.modifiedItem as PrintingQueueItem,
                    updateState = updateState
                )
            }
            is PrintingAction.SkipProcessor -> {
                confirmationUseCase.skipProcessor(
                    requestId = action.requestId,
                    printingQueue = printingQueue
                )
            }
            is PrintingAction.SkipProcessorOnError -> {
                confirmationUseCase.skipProcessorOnError(
                    requestId = action.requestId,
                    printingQueue = printingQueue
                )
            }
            
            // Error handling actions
            is PrintingAction.HandleFailedPrinting -> {
                errorHandlingUseCase.handleFailedPrinting(
                    requestId = action.requestId,
                    action = action.action,
                    printingQueue = printingQueue,
                    emitUiEvent = emitUiEvent,
                    updateState = updateState
                )
            }

            is PrintingAction.ConfirmPrinterNetworkInfo -> {
                confirmationUseCase.confirmPrinterNetworkInfo(
                    requestId = action.requestId,
                    networkInfo = action.networkInfo,
                    printingQueue = printingQueue,
                    updateState = updateState
                )
            }

            // Internal actions triggered by events
            is PrintingAction.ProcessingStateChanged -> {
                action.state?.let { state ->
                    stateManagementUseCase.handleProcessingStateChange(
                        state = state,
                        emitUiEvent = emitUiEvent,
                        updateState = updateState
                    )
                }
                null // No side effect needed
            }
            is PrintingAction.QueueInputRequested -> {
                stateManagementUseCase.handleQueueInputRequest(
                    request = action.request,
                    printingQueue = printingQueue,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            is PrintingAction.ProcessorInputRequested -> {
                stateManagementUseCase.handleProcessorInputRequest(
                    request = action.request,
                    updateState = updateState
                )
                null // No side effect needed
            }
        }
    }
}
