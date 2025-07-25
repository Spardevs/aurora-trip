package br.com.ticpass.pos.queue.payment.state

import android.util.Log
import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.payment.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.queue.payment.usecases.ProcessorConfirmationUseCase
import br.com.ticpass.pos.queue.payment.usecases.QueueManagementUseCase
import br.com.ticpass.pos.queue.payment.usecases.StateManagementUseCase
import javax.inject.Inject

/**
 * Reducer class for handling state transitions and side effects in the InteractivePaymentViewModel
 * Refactored to use use case classes for better maintainability and testability
 */
class InteractivePaymentReducer @Inject constructor(
    private val queueManagementUseCase: QueueManagementUseCase,
    private val errorHandlingUseCase: ErrorHandlingUseCase,
    private val processorConfirmationUseCase: ProcessorConfirmationUseCase,
    private val stateManagementUseCase: StateManagementUseCase
) {
    
    private lateinit var emitUiEvent: (UiEvent) -> Unit
    private lateinit var updateState: (UiState) -> Unit
    
    /**
     * Initialize the reducer with callback functions from the ViewModel
     */
    fun initialize(
        emitUiEvent: (UiEvent) -> Unit,
        updateState: (UiState) -> Unit
    ) {
        this.emitUiEvent = emitUiEvent
        this.updateState = updateState
    }
    /**
     * Reduce an action to a side effect and update the UI state
     * @param action The action to reduce
     * @param paymentQueue The payment queue to operate on
     * @return A side effect to execute, or null if no side effect is needed
     */
    fun reduce(
        action: Action,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>
    ): SideEffect? {
        return when (action) {
            // Queue management actions
            is Action.StartProcessing -> {
                queueManagementUseCase.startProcessing(paymentQueue, emitUiEvent)
            }
            is Action.EnqueuePayment -> {
                queueManagementUseCase.enqueuePayment(
                    amount = action.amount,
                    commission = action.commission,
                    method = action.method,
                    processorType = action.processorType,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is Action.CancelPayment -> {
                queueManagementUseCase.cancelPayment(
                    paymentId = action.paymentId,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is Action.CancelAllPayments -> {
                queueManagementUseCase.cancelAllPayments(paymentQueue, emitUiEvent)
            }
            
            // Processor confirmation actions
            is Action.ConfirmNextProcessor -> {
                processorConfirmationUseCase.confirmNextProcessor(
                    requestId = action.requestId,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            is Action.ConfirmNextProcessorWithModifiedPayment -> {
                processorConfirmationUseCase.confirmNextProcessorWithModifiedPayment(
                    requestId = action.requestId,
                    modifiedAmount = action.modifiedAmount,
                    modifiedMethod = action.modifiedMethod,
                    modifiedProcessorType = action.modifiedProcessorType,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            is Action.SkipProcessor -> {
                processorConfirmationUseCase.skipProcessor(
                    requestId = action.requestId,
                    paymentQueue = paymentQueue
                )
            }
            
            // Error handling actions
            is Action.HandleFailedPayment -> {
                errorHandlingUseCase.handleFailedPayment(
                    requestId = action.requestId,
                    action = action.action,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent,
                    updateState = updateState
                )
            }
            
            // Receipt printing actions
            is Action.ConfirmCustomerReceiptPrinting -> {
                processorConfirmationUseCase.confirmCustomerReceiptPrinting(
                    requestId = action.requestId,
                    shouldPrint = action.shouldPrint,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            
            // PIX payment actions
            is Action.ConfirmMerchantPixKey -> {
                processorConfirmationUseCase.confirmMerchantPixKey(
                    requestId = action.requestId,
                    pixKey = action.pixKey,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            
            // Internal actions triggered by events
            is Action.ProcessingStateChanged -> {
                action.state?.let { state ->
                    stateManagementUseCase.handleProcessingStateChange(
                        state = state,
                        emitUiEvent = emitUiEvent,
                        updateState = updateState
                    )
                }
                null // No side effect needed
            }
            is Action.QueueInputRequested -> {
                stateManagementUseCase.handleQueueInputRequest(
                    request = action.request,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            is Action.ProcessorInputRequested -> {
                stateManagementUseCase.handleProcessorInputRequest(
                    request = action.request,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            // Transactionless mode actions
            is Action.UpdateAllProcessorTypes -> {
                queueManagementUseCase.updateAllProcessorTypes(
                    useTransactionless = action.useTransactionless,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent
                )
            }
        }
    }
}
