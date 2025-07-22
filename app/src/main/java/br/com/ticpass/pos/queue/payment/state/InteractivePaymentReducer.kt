package br.com.ticpass.pos.queue.payment.state

import br.com.ticpass.pos.queue.ErrorHandlingAction
import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.PaymentQueueInputRequest
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.QueueInputResponse
import br.com.ticpass.pos.queue.payment.PaymentQueueInputResponse
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import java.util.UUID

/**
 * Reducer class for handling state transitions and side effects in the InteractivePaymentViewModel
 */
class InteractivePaymentReducer(
    private val emitUiEvent: (UiEvent) -> Unit,
    private val updateState: (UiState) -> Unit
) {
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
            // Queue actions
            is Action.StartProcessing -> {
                emitUiEvent(UiEvent.ShowToast("Starting payment processing"))
                SideEffect.StartProcessingQueue { paymentQueue.startProcessing() }
            }
            is Action.EnqueuePayment -> {
                val paymentItem = ProcessingPaymentQueueItem(
                    id = UUID.randomUUID().toString(),
                    amount = action.amount,
                    commission = action.commission,
                    method = action.method,
                    priority = 10,
                    processorType = action.processorType
                )
                emitUiEvent(UiEvent.ShowToast("Payment added to queue"))
                SideEffect.EnqueuePaymentItem { paymentQueue.enqueue(paymentItem) }
            }
            is Action.CancelPayment -> {
                SideEffect.RemovePaymentItem {
                    val item = paymentQueue.queueState.value.find { it.id == action.paymentId }
                    if (item != null) {
                        paymentQueue.remove(item)
                        emitUiEvent(UiEvent.ShowToast("Payment cancelled"))
                    }
                }
            }
            is Action.CancelAllPayments -> {
                emitUiEvent(UiEvent.ShowToast("All payments cancelled"))
                SideEffect.RemoveAllPaymentItems { paymentQueue.removeAll() }
            }
            
            // Processor input actions
            is Action.ConfirmNextProcessor -> {
                updateState(UiState.Processing)
                SideEffect.ProvideQueueInput { 
                    paymentQueue.provideQueueInput(QueueInputResponse.proceed(action.requestId)) 
                }
            }
            is Action.ConfirmNextProcessorWithModifiedPayment -> {
                updateState(UiState.Processing)
                val response = PaymentQueueInputResponse.proceedWithModifiedPayment(
                    requestId = action.requestId,
                    modifiedAmount = action.modifiedAmount,
                    modifiedMethod = action.modifiedMethod,
                    modifiedProcessorType = action.modifiedProcessorType
                )
                SideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
            }
            is Action.SkipProcessor -> {
                SideEffect.ProvideQueueInput { 
                    paymentQueue.provideQueueInput(QueueInputResponse.skip(action.requestId)) 
                }
            }
            
            // Error handling actions
            is Action.HandleFailedPayment -> {
                val response = when (action.action) {
                    ErrorHandlingAction.RETRY_IMMEDIATELY -> QueueInputResponse.retryImmediately(action.requestId)
                    ErrorHandlingAction.RETRY_LATER -> QueueInputResponse.retryLater(action.requestId)
                    ErrorHandlingAction.ABORT_CURRENT -> QueueInputResponse.abortCurrentProcessor(action.requestId)
                    ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.abortAllProcessors(action.requestId)
                }
                
                // Update UI state for actions that continue processing
                if (action.action == ErrorHandlingAction.RETRY_IMMEDIATELY || action.action == ErrorHandlingAction.RETRY_LATER) {
                    updateState(UiState.Processing)
                    
                    // Emit appropriate UI event
                    val message = if (action.action == ErrorHandlingAction.RETRY_IMMEDIATELY) {
                        "Retrying payment immediately"
                    } else {
                        "Payment moved to the end of the queue"
                    }
                    emitUiEvent(UiEvent.ShowToast(message))
                } else if (action.action == ErrorHandlingAction.ABORT_CURRENT) {
                    emitUiEvent(UiEvent.ShowToast("Skipped current payment"))
                } else if (action.action == ErrorHandlingAction.ABORT_ALL) {
                    emitUiEvent(UiEvent.ShowToast("Cancelled all payments"))
                }
                
                SideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
            }
            
            // Receipt printing actions
            is Action.ConfirmCustomerReceiptPrinting -> {
                updateState(UiState.Processing)
                // Create a standard input response with the print choice as the value
                val response = QueueInputResponse(action.requestId, action.shouldPrint)
                SideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
            }
            
            // Internal actions triggered by events
            is Action.ProcessingStateChanged -> {
                when (action.state) {
                    is ProcessingState.ItemProcessing -> {
                        updateState(UiState.Processing)
                        // Emit event that processing started for this item
                        val item = action.state.item
                        if (item is ProcessingPaymentQueueItem) {
                            emitUiEvent(UiEvent.ShowToast("Processing payment ${item.id}"))
                        }
                    }
                    is ProcessingState.ItemDone -> {
                        // Emit event that item was completed successfully
                        val item = action.state.item
                        if (item is ProcessingPaymentQueueItem) {
                            emitUiEvent(UiEvent.PaymentCompleted(item.id, item.amount))
                        }
                    }
                    is ProcessingState.QueueIdle -> {
                        updateState(UiState.Idle)
                    }
                    is ProcessingState.ItemFailed -> {
                        updateState(UiState.Error(action.state.error))
                        // Emit event that item failed
                        val item = action.state.item
                        if (item is ProcessingPaymentQueueItem) {
                            emitUiEvent(UiEvent.PaymentFailed(item.id, action.state.error))
                        }
                    }
                    null -> { /* No UI state change when state is null */ }
                    else -> { /* No UI state change for other processing states */ }
                }
                null // No side effect needed
            }
            is Action.QueueInputRequested -> {
                when (action.request) {
                    is PaymentQueueInputRequest.CONFIRM_NEXT_PAYMENT -> {
                        updateState(UiState.ConfirmNextPaymentProcessor(
                            requestId = action.request.id,
                            currentItemIndex = action.request.currentItemIndex,
                            totalItems = action.request.totalItems,
                            currentAmount = action.request.currentAmount,
                            currentMethod = action.request.currentMethod,
                            currentProcessorType = action.request.currentProcessorType
                        ))
                    }
                    else -> {
                        // Handle other request types if needed
                    }
                }
                null // No side effect needed
            }
        }
    }
}
