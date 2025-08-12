package br.com.ticpass.pos.feature.payment.state

import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.feature.payment.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.feature.payment.usecases.ConfirmationUseCase
import br.com.ticpass.pos.feature.payment.usecases.QueueManagementUseCase
import br.com.ticpass.pos.feature.payment.usecases.StateManagementUseCase
import javax.inject.Inject

/**
 * Reducer class for handling state transitions and side effects in the PaymentProcessingViewModel
 * Refactored to use use case classes for better maintainability and testability
 */
class PaymentProcessingReducer @Inject constructor(
    private val queueManagementUseCase: QueueManagementUseCase,
    private val errorHandlingUseCase: ErrorHandlingUseCase,
    private val confirmationUseCase: ConfirmationUseCase,
    private val stateManagementUseCase: StateManagementUseCase
) {
    
    private lateinit var emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    private lateinit var updateState: (PaymentProcessingUiState) -> Unit
    
    /**
     * Initialize the reducer with callback functions from the ViewModel
     */
    fun initialize(
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit,
        updateState: (PaymentProcessingUiState) -> Unit
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
        action: PaymentProcessingAction,
        paymentQueue: HybridQueueManager<PaymentProcessingQueueItem, PaymentProcessingEvent>
    ): PaymentProcessingSideEffect? {
        return when (action) {
            // Queue management actions
            is PaymentProcessingAction.StartProcessing -> {
                queueManagementUseCase.startProcessing(paymentQueue, emitUiEvent)
            }
            is PaymentProcessingAction.EnqueuePayment -> {
                queueManagementUseCase.enqueuePayment(
                    amount = action.amount,
                    commission = action.commission,
                    method = action.method,
                    isTransactionless = action.isTransactionless,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is PaymentProcessingAction.CancelPayment -> {
                queueManagementUseCase.cancelPayment(
                    paymentId = action.paymentId,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent
                )
            }
            is PaymentProcessingAction.ClearQueue -> {
                queueManagementUseCase.clearQueue(paymentQueue, emitUiEvent)
            }
            is PaymentProcessingAction.AbortCurrentPayment -> {
                queueManagementUseCase.abortCurrentPayment(paymentQueue, emitUiEvent)
            }
            
            // Processor confirmation actions
            is PaymentProcessingAction.ConfirmProcessor<*> -> {
                confirmationUseCase.confirmProcessor(
                    requestId = action.requestId,
                    queue = paymentQueue,
                    modifiedItem = action.modifiedItem as PaymentProcessingQueueItem,
                    updateState = updateState
                )
            }
            is PaymentProcessingAction.SkipProcessor -> {
                confirmationUseCase.skipProcessor(
                    requestId = action.requestId,
                    paymentQueue = paymentQueue
                )
            }
            is PaymentProcessingAction.SkipProcessorOnError -> {
                confirmationUseCase.skipProcessorOnError(
                    requestId = action.requestId,
                    paymentQueue = paymentQueue
                )
            }
            
            // Error handling actions
            is PaymentProcessingAction.HandleFailedPayment -> {
                errorHandlingUseCase.handleFailedPayment(
                    requestId = action.requestId,
                    action = action.action,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent,
                    updateState = updateState
                )
            }
            
            // Receipt printing actions
            is PaymentProcessingAction.ConfirmCustomerReceiptPrinting -> {
                confirmationUseCase.confirmCustomerReceiptPrinting(
                    requestId = action.requestId,
                    shouldPrint = action.shouldPrint,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            
            // PIX payment actions
            is PaymentProcessingAction.ConfirmMerchantPixKey -> {
                confirmationUseCase.confirmMerchantPixKey(
                    requestId = action.requestId,
                    pixKey = action.pixKey,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            
            is PaymentProcessingAction.ConfirmMerchantPixHasBeenPaid -> {
                confirmationUseCase.confirmMerchantPixHasBeenPaid(
                    requestId = action.requestId,
                    didPay = action.didPay,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
            }
            
            // Internal actions triggered by events
            is PaymentProcessingAction.ProcessingStateChanged -> {
                action.state?.let { state ->
                    stateManagementUseCase.handleProcessingStateChange(
                        state = state,
                        emitUiEvent = emitUiEvent,
                        updateState = updateState
                    )
                }
                null // No side effect needed
            }
            is PaymentProcessingAction.QueueInputRequested -> {
                stateManagementUseCase.handleQueueInputRequest(
                    request = action.request,
                    paymentQueue = paymentQueue,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            is PaymentProcessingAction.ProcessorInputRequested -> {
                stateManagementUseCase.handleProcessorInputRequest(
                    request = action.request,
                    updateState = updateState
                )
                null // No side effect needed
            }
            
            // Transactionless mode actions
            is PaymentProcessingAction.ToggleTransactionless -> {
                queueManagementUseCase.toggleTransactionless(
                    useTransactionless = action.useTransactionless,
                    paymentQueue = paymentQueue,
                    emitUiEvent = emitUiEvent
                )
            }
        }
    }
}
