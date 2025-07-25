package br.com.ticpass.pos.queue.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.queue.ErrorHandlingAction
import br.com.ticpass.pos.queue.ProcessorStartMode
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.payment.processors.PaymentMethodProcessorMapper
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType
import br.com.ticpass.pos.queue.payment.state.Action
import br.com.ticpass.pos.queue.payment.state.InteractivePaymentReducer
import br.com.ticpass.pos.queue.payment.state.SideEffect
import br.com.ticpass.pos.queue.payment.state.UiEvent
import br.com.ticpass.pos.queue.payment.state.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Interactive Payment ViewModel
 * Example ViewModel that demonstrates handling input requests from payment processors
 * 
 * This ViewModel uses a modular architecture with:
 * - Actions: Represent user interactions and events
 * - Reducer: Handles state transitions and side effects
 * - UiState: Represents the current UI state
 * - UiEvent: Represents one-time events to be consumed by the UI
 * - SideEffect: Represents operations that don't directly affect the UI state
 */
@HiltViewModel
class InteractivePaymentViewModel @Inject constructor(
    paymentQueueFactory: ProcessingPaymentQueueFactory,
    processingPaymentStorage: ProcessingPaymentStorage,
    private val reducer: InteractivePaymentReducer
) : ViewModel() {
    
    // Queue Setup and Configuration
    // Initialize the queue with viewModelScope
    private val paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent> = paymentQueueFactory.createDynamicPaymentQueue(
        storage = processingPaymentStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode = ProcessorStartMode.CONFIRMATION,
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
    
    // UI State Management
    // UI Events flow for one-time events
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    /**
     * Emit a one-time UI event
     */
    private fun emitUiEvent(event: UiEvent) {
        launchInViewModelScope {
            _uiEvents.emit(event)
        }
    }
    
    // UI State flow for persistent state
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * Update the UI state
     */
    private fun updateState(newState: UiState) {
        _uiState.value = newState
    }
    
    // Reducer for handling actions and producing side effects (injected)
    init {
        // Initialize the reducer with callback functions
        reducer.initialize(
            emitUiEvent = ::emitUiEvent,
            updateState = ::updateState
        )
        
        // Observe processor input requests
        viewModelScope.launch {
            paymentQueue.processor.inputRequests.collect { request ->
                Log.d("InteractivePaymentViewModel", "Processor input request received: ${request::class.simpleName}")
                dispatch(Action.ProcessorInputRequested(request))
            }
        }
    }
    
    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: Action) {
        val sideEffect = reducer.reduce(action, paymentQueue)
        sideEffect?.let { executeSideEffect(it) }
    }

    /**
     * Execute a side effect
     * All side effects are executed in the ViewModel scope
     */
    private fun executeSideEffect(sideEffect: SideEffect) {
        launchInViewModelScope {
            sideEffect.scope()
        }
    }
    
    // Initialization and Event Handling
    
    init {
        // Observe processing state changes
        launchInViewModelScope {
            paymentQueue.processingState.collectLatest { state ->
                dispatch(Action.ProcessingStateChanged(state))
            }
        }
        
        // Observe queue input requests
        launchInViewModelScope {
            paymentQueue.queueInputRequests.collectLatest { request ->
                dispatch(Action.QueueInputRequested(request))
            }
        }
    }
    
    // Public API
    
    /**
     * Start processing the payment queue
     */
    fun startProcessing() {
        dispatch(Action.StartProcessing)
    }
    
    /**
     * Process a payment with the specified processor type
     * Uses the processor type from the mapper or the provided override
     */
    fun enqueuePayment(
        amount: Int,
        commission: Int = 0,
        method: SystemPaymentMethod,
        processorType: PaymentProcessorType = PaymentMethodProcessorMapper.getProcessorTypeForMethod(method)
    ) {
        dispatch(Action.EnqueuePayment(amount, commission, method, processorType))
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
    
    // Processor-Level Input Handling
    
    /**
     * Confirm customer receipt printing (processor-level input request)
     */
    fun confirmCustomerReceiptPrinting(requestId: String, shouldPrint: Boolean) {
        dispatch(Action.ConfirmCustomerReceiptPrinting(requestId, shouldPrint))
    }
    
    /**
     * Confirm personal PIX key (processor-level input request)
     */
    fun confirmPersonalPixKey(requestId: String, pixKey: String) {
        dispatch(Action.ConfirmPersonalPixKey(requestId, pixKey))
    }
    
    // Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun confirmNextProcessor(requestId: String) {
        dispatch(Action.ConfirmNextProcessor(requestId))
    }
    
    /**
     * Update processor type for all queued items
     * Used when toggling transactionless mode
     */
    fun updateAllProcessorTypes(useTransactionless: Boolean) {
        dispatch(Action.UpdateAllProcessorTypes(useTransactionless))
    }
    
    /**
     * Confirm the next processor with modified payment details
     */
    fun confirmNextProcessorWithModifiedPayment(
        requestId: String,
        modifiedAmount: Int,
        modifiedMethod: SystemPaymentMethod,
        modifiedProcessorType: PaymentProcessorType = PaymentMethodProcessorMapper.getProcessorTypeForMethod(modifiedMethod)
    ) {
        dispatch(Action.ConfirmNextProcessorWithModifiedPayment(
            requestId,
            modifiedAmount,
            modifiedMethod,
            modifiedProcessorType
        ))
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        dispatch(Action.SkipProcessor(requestId))
    }
    
    // Error Handling
    
    /**
     * Handle a failed payment with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    private fun handleFailedPayment(requestId: String, action: ErrorHandlingAction) {
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
}
