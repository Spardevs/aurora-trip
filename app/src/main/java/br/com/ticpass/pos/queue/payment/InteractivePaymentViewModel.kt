package br.com.ticpass.pos.queue.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.queue.ErrorHandlingAction
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.PaymentQueueInputRequest
import br.com.ticpass.pos.queue.payment.PaymentQueueInputResponse
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.QueueConfirmationMode
import br.com.ticpass.pos.queue.QueueInputRequest
import br.com.ticpass.pos.queue.QueueInputResponse
import br.com.ticpass.pos.queue.HybridQueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Interactive Payment ViewModel
 * Example ViewModel that demonstrates handling input requests from payment processors
 */
@HiltViewModel
class InteractivePaymentViewModel @Inject constructor(
    paymentQueueFactory: ProcessingPaymentQueueFactory,
    processingPaymentStorage: ProcessingPaymentStorage
) : ViewModel() {
    
    //region Queue Setup and Configuration
    
    // Initialize the queue with viewModelScope
    private val paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent> = paymentQueueFactory.createDynamicPaymentQueue(
        storage = processingPaymentStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        queueConfirmationMode = QueueConfirmationMode.CONFIRMATION,
        scope = viewModelScope
    )
    
    /**
     * Helper function to launch coroutines in the viewModelScope with standard error handling
     */
    private fun launchInViewModelScope(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                // Common error handling
                updateState(UiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }
    
    // Expose queue states to UI
    val queueState = paymentQueue.queueState
    val processingState = paymentQueue.processingState
    val processingPaymentEvents: SharedFlow<ProcessingPaymentEvent> = paymentQueue.processorEvents
    
    //endregion
    
    //region UI State Management
    
    /**
     * Represents an action that can be dispatched to the ViewModel
     * Actions trigger state transitions and side effects
     */
    sealed class Action {
        // Queue actions
        object StartProcessing : Action()
        data class EnqueuePayment(
            val amount: Int,
            val commission: Int,
            val method: SystemPaymentMethod,
            val processorType: String
        ) : Action()
        data class CancelPayment(val paymentId: String) : Action()
        object CancelAllPayments : Action()
        
        // Processor-level input actions
        data class ConfirmCustomerReceiptPrinting(val requestId: String, val doPrint: Boolean) : Action()
        data class CancelInput(val requestId: String) : Action()
        
        // Queue-level input actions
        data class ConfirmNextProcessor(val requestId: String) : Action()
        data class ConfirmNextProcessorWithModifiedPayment(
            val requestId: String,
            val modifiedAmount: Int,
            val modifiedMethod: SystemPaymentMethod,
            val modifiedProcessorType: String
        ) : Action()
        data class SkipProcessor(val requestId: String) : Action()
        
        // Error handling actions
        data class HandleFailedPayment(val requestId: String, val action: ErrorHandlingAction) : Action()
        
        // Internal actions triggered by events
        data class ProcessingStateChanged(val state: ProcessingState<ProcessingPaymentQueueItem>?) : Action()
        data class ProcessorInputRequested(val request: InputRequest) : Action()
        data class QueueInputRequested(val request: QueueInputRequest) : Action()
    }
    
    // Current UI state - tracks what input the UI should be showing
    sealed class UiState {
        object Idle : UiState()
        object Processing : UiState()
        data class Error(val event: ProcessingErrorEvent) : UiState()
        
        // Processor-level input requests
        data class ConfirmCustomerReceiptPrinting(val requestId: String, val doPrint: Boolean) : UiState()
        
        // Generic processor confirmation (no payment details)
        data class ConfirmNextProcessor(
            val requestId: String, 
            val currentItemIndex: Int, 
            val totalItems: Int
        ) : UiState()
        
        // Payment-specific processor confirmation with payment details
        data class ConfirmNextPaymentProcessor(
            val requestId: String, 
            val currentItemIndex: Int, 
            val totalItems: Int,
            val currentAmount: Int,
            val currentMethod: SystemPaymentMethod,
            val currentProcessorType: String
        ) : UiState()
        
        data class ErrorRetryOrSkip(val requestId: String, val error: ProcessingErrorEvent) : UiState()
    }
    
    /**
     * Represents a side effect that should be executed as a result of an action
     * Side effects are one-time operations that don't directly affect the UI state
     */
    private sealed class SideEffect {
        data class StartProcessingQueue(val scope: suspend () -> Unit) : SideEffect()
        data class EnqueuePaymentItem(val item: ProcessingPaymentQueueItem, val scope: suspend () -> Unit) : SideEffect()
        data class RemovePaymentItem(val itemId: String, val scope: suspend () -> Unit) : SideEffect()
        data class RemoveAllPaymentItems(val scope: suspend () -> Unit) : SideEffect()
        data class ProvideProcessorInput(val response: InputResponse, val scope: suspend () -> Unit) : SideEffect()
        data class ProvideQueueInput(val response: QueueInputResponse, val scope: suspend () -> Unit) : SideEffect()
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * Single entry point for UI state updates
     * All state changes should go through this method to ensure consistency
     */
    private fun updateState(newState: UiState) {
        _uiState.value = newState
    }
    
    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: Action) {
        val sideEffect = reduce(action)
        sideEffect?.let { executeSideEffect(it) }
    }
    
    /**
     * Reducer function that handles state transitions based on actions
     * Returns a side effect to be executed, if any
     */
    private fun reduce(action: Action): SideEffect? {
        return when (action) {
            // Queue actions
            is Action.StartProcessing -> {
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
                SideEffect.EnqueuePaymentItem(paymentItem) { paymentQueue.enqueue(paymentItem) }
            }
            is Action.CancelPayment -> {
                SideEffect.RemovePaymentItem(action.paymentId) {
                    val item = paymentQueue.queueState.value.find { it.id == action.paymentId }
                    if (item != null) {
                        paymentQueue.remove(item)
                    }
                }
            }
            is Action.CancelAllPayments -> {
                SideEffect.RemoveAllPaymentItems { paymentQueue.removeAll() }
            }
            
            // Processor-level input actions
            is Action.ConfirmCustomerReceiptPrinting -> {
                updateState(UiState.Processing)
                SideEffect.ProvideProcessorInput(
                    InputResponse(action.requestId, action.doPrint)
                ) { paymentQueue.processor.provideInput(InputResponse(action.requestId, action.doPrint)) }
            }
            is Action.CancelInput -> {
                SideEffect.ProvideProcessorInput(
                    InputResponse.canceled(action.requestId)
                ) { paymentQueue.processor.provideInput(InputResponse.canceled(action.requestId)) }
            }
            
            // Queue-level input actions
            is Action.ConfirmNextProcessor -> {
                updateState(UiState.Processing)
                SideEffect.ProvideQueueInput(
                    QueueInputResponse.proceed(action.requestId)
                ) { paymentQueue.provideQueueInput(QueueInputResponse.proceed(action.requestId)) }
            }
            is Action.ConfirmNextProcessorWithModifiedPayment -> {
                updateState(UiState.Processing)
                val response = PaymentQueueInputResponse.proceedWithModifiedPayment(
                    requestId = action.requestId,
                    modifiedAmount = action.modifiedAmount,
                    modifiedMethod = action.modifiedMethod,
                    modifiedProcessorType = action.modifiedProcessorType
                )
                SideEffect.ProvideQueueInput(response) { paymentQueue.provideQueueInput(response) }
            }
            is Action.SkipProcessor -> {
                SideEffect.ProvideQueueInput(
                    QueueInputResponse.skip(action.requestId)
                ) { paymentQueue.provideQueueInput(QueueInputResponse.skip(action.requestId)) }
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
                }
                
                SideEffect.ProvideQueueInput(response) { paymentQueue.provideQueueInput(response) }
            }
            
            // Internal actions triggered by events
            is Action.ProcessingStateChanged -> {
                when (action.state) {
                    is ProcessingState.ItemProcessing -> updateState(UiState.Processing)
                    is ProcessingState.QueueIdle -> updateState(UiState.Idle)
                    is ProcessingState.ItemFailed -> updateState(UiState.Error(action.state.error))
                    null -> { /* No UI state change when state is null */ }
                    else -> { /* No UI state change for other processing states */ }
                }
                null // No side effect needed
            }
            is Action.ProcessorInputRequested -> {
                when (action.request) {
                    is InputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
                        updateState(UiState.ConfirmCustomerReceiptPrinting(
                            requestId = action.request.id,
                            doPrint = false // Default value, user can change via UI
                        ))
                    }
                }
                null // No side effect needed
            }
            is Action.QueueInputRequested -> {
                when (action.request) {
                    is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                        updateState(UiState.ConfirmNextProcessor(
                            requestId = action.request.id,
                            currentItemIndex = action.request.currentItemIndex,
                            totalItems = action.request.totalItems
                        ))
                    }
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
                    is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                        updateState(UiState.ErrorRetryOrSkip(
                            requestId = action.request.id,
                            error = action.request.error
                        ))
                    }
                }
                null // No side effect needed
            }
        }
    }
    
    /**
     * Execute a side effect using the viewModelScope
     */
    private fun executeSideEffect(effect: SideEffect) {
        when (effect) {
            is SideEffect.StartProcessingQueue -> launchInViewModelScope { effect.scope() }
            is SideEffect.EnqueuePaymentItem -> launchInViewModelScope { effect.scope() }
            is SideEffect.RemovePaymentItem -> launchInViewModelScope { effect.scope() }
            is SideEffect.RemoveAllPaymentItems -> launchInViewModelScope { effect.scope() }
            is SideEffect.ProvideProcessorInput -> launchInViewModelScope { effect.scope() }
            is SideEffect.ProvideQueueInput -> launchInViewModelScope { effect.scope() }
        }
    }
    
    //endregion
    
    //region Initialization and Event Handling
    
    init {
        // Observe processing state
        launchInViewModelScope {
            processingState.collectLatest { state ->
                  dispatch(Action.ProcessingStateChanged(state))
            }
        }
        
        // Listen for input requests from the processor
        launchInViewModelScope {
            (paymentQueue.processor.inputRequests).collectLatest { request ->
                dispatch(Action.ProcessorInputRequested(request))
            }
        }
        
        // Listen for queue-level input requests
        launchInViewModelScope {
            paymentQueue.queueInputRequests.collectLatest { request ->
                dispatch(Action.QueueInputRequested(request))
            }
        }
    }
    
    //endregion

    //region Queue Operations
    
    /**
     * Start processing the payment queue
     */
    fun startProcessing() {
        dispatch(Action.StartProcessing)
    }
    
    /**
     * Process a payment with the specified processor type
     */
    fun enqueuePayment(
        amount: Int,
        commission: Int,
        method: SystemPaymentMethod,
        processorType: String = "acquirer", // "acquirer", "cash", or "transactionless"
    ) {
        dispatch(Action.EnqueuePayment(
            amount = amount,
            commission = commission,
            method = method,
            processorType = processorType
        ))
    }
    
    /**
     * Cancel a payment
     */
    fun cancelPayment(paymentId: String) {
        dispatch(Action.CancelPayment(paymentId))
    }
    
    /**
     * Cancel all payments
     * Uses a single operation to remove all items at once
     */
    fun cancelAllPayments() {
        dispatch(Action.CancelAllPayments)
    }
    
    //endregion

    //region Processor-Level Input Handling
    
    /**
     * Confirm customer receipt printing (processor-level input request)
     */
    fun confirmCustomerReceiptPrinting(requestId: String, doPrint: Boolean) {
        dispatch(Action.ConfirmCustomerReceiptPrinting(requestId, doPrint))
    }

    /**
     * Cancel the current processor-level input request
     */
    fun cancelInput(requestId: String) {
        dispatch(Action.CancelInput(requestId))
    }
    
    //endregion
    
    //region Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun confirmNextProcessor(requestId: String) {
        dispatch(Action.ConfirmNextProcessor(requestId))
    }
    
    /**
     * Confirm the next processor with modified payment details
     */
    fun confirmNextProcessorWithModifiedPayment(
        requestId: String,
        modifiedAmount: Int,
        modifiedMethod: SystemPaymentMethod,
        modifiedProcessorType: String
    ) {
        dispatch(Action.ConfirmNextProcessorWithModifiedPayment(
            requestId = requestId,
            modifiedAmount = modifiedAmount,
            modifiedMethod = modifiedMethod,
            modifiedProcessorType = modifiedProcessorType
        ))
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        dispatch(Action.SkipProcessor(requestId))
    }
    
    //endregion
    
    //region Error Handling
    
    /**
     * Handle a failed payment with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    fun handleFailedPayment(requestId: String, action: ErrorHandlingAction) {
        dispatch(Action.HandleFailedPayment(requestId, action))
    }
    
    /**
     * Retry a failed payment immediately (queue-level input request)
     * This will retry the same processor without moving the item
     */
    fun retryFailedPaymentImmediately(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.RETRY_IMMEDIATELY)
    }
    
    /**
     * Retry a failed payment later (queue-level input request)
     * This will move the item to the end of the queue and continue with the next item
     */
    fun retryFailedPaymentLater(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.RETRY_LATER)
    }
    
    /**
     * Abort the current processor and continue with the next item (queue-level input request)
     */
    fun abortCurrentProcessor(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.ABORT_CURRENT)
    }
    
    /**
     * Abort all processors and stop processing (queue-level input request)
     */
    fun abortAllProcessors(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.ABORT_ALL)
    }
    
    //endregion
}
