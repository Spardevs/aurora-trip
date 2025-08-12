package br.com.ticpass.pos.feature.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingAction
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingReducer
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.payment.utils.PaymentProcessingQueueFactory
import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingStorage
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingSideEffect
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Payment Processing ViewModel
 * Example ViewModel that demonstrates handling input requests from payment processors
 * 
 * This ViewModel uses a modular architecture with:
 * - Actions: Represent user interactions and events
 * - Reducer: Handles state transitions and side effects
 * - PaymentProcessingUiState: Represents the current UI state
 * - PaymentProcessingUiEvent: Represents one-time events to be consumed by the UI
 * - PaymentProcessingSideEffect: Represents operations that don't directly affect the UI state
 */
@HiltViewModel
class PaymentProcessingViewModel @Inject constructor(
    paymentQueueFactory: PaymentProcessingQueueFactory,
    processingPaymentStorage: PaymentProcessingStorage,
    private val reducer: PaymentProcessingReducer
) : ViewModel() {
    
    // Queue Setup and Configuration
    // Initialize the queue with viewModelScope
    private val paymentQueue: HybridQueueManager<PaymentProcessingQueueItem, PaymentProcessingEvent> = paymentQueueFactory.createDynamicPaymentQueue(
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
                updateState(PaymentProcessingUiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }
    
    // Expose queue states to UI
    val queueState = paymentQueue.queueState
    val fullSize = paymentQueue.fullSize
    val enqueuedSize = paymentQueue.enqueuedSize
    val currentIndex = paymentQueue.currentIndex
    val processingState = paymentQueue.processingState
    val processingPaymentEvents: SharedFlow<PaymentProcessingEvent> = paymentQueue.processorEvents
    
    // UI State Management
    // UI Events flow for one-time events
    private val _uiEvents = MutableSharedFlow<PaymentProcessingUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    /**
     * Emit a one-time UI event
     */
    private fun emitUiEvent(event: PaymentProcessingUiEvent) {
        launchInViewModelScope {
            _uiEvents.emit(event)
        }
    }
    
    // UI State flow for persistent state
    private val _uiState = MutableStateFlow<PaymentProcessingUiState>(PaymentProcessingUiState.Idle)
    val uiState: StateFlow<PaymentProcessingUiState> = _uiState.asStateFlow()
    
    /**
     * Update the UI state
     */
    private fun updateState(newState: PaymentProcessingUiState) {
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
            paymentQueue.processor.userInputRequests.collect { request ->
                Log.d("PaymentProcessingViewModel", "Processor input request received: ${request::class.simpleName}")
                dispatch(PaymentProcessingAction.ProcessorInputRequested(request))
            }
        }
    }
    
    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: PaymentProcessingAction) {
        val sideEffect = reducer.reduce(action, paymentQueue)
        sideEffect?.let { executeSideEffect(it) }
    }

    /**
     * Execute a side effect
     * All side effects are executed in the ViewModel scope
     */
    private fun executeSideEffect(sideEffect: PaymentProcessingSideEffect) {
        launchInViewModelScope {
            sideEffect.scope()
        }
    }
    
    // Initialization and Event Handling
    
    init {
        // Observe processing state changes
        launchInViewModelScope {
            paymentQueue.processingState.collectLatest { state ->
                dispatch(PaymentProcessingAction.ProcessingStateChanged(state))
            }
        }
        
        // Observe queue input requests
        launchInViewModelScope {
            paymentQueue.queueInputRequests.collectLatest { request ->
                dispatch(PaymentProcessingAction.QueueInputRequested(request))
            }
        }
    }
    
    // Public API
    
    /**
     * Start processing the payment queue
     */
    fun startProcessing() {
        dispatch(PaymentProcessingAction.StartProcessing)
    }
    
    /**
     * Process a payment with the specified processor type
     * Uses the processor type from the mapper or the provided override
     */
    fun enqueuePayment(
        amount: Int,
        commission: Int = 0,
        method: SystemPaymentMethod,
        isTransactionless: Boolean,
    ) {
        dispatch(PaymentProcessingAction.EnqueuePayment(amount, commission, method, isTransactionless))
    }
    
    /**
     * Cancel a payment
     */
    fun cancelPayment(paymentId: String) {
        dispatch(PaymentProcessingAction.CancelPayment(paymentId))
    }
    
    /**
     * Cancel all payments
     * Uses a single operation to remove all items at once
     */
    fun cancelAllPayments() {
        dispatch(PaymentProcessingAction.ClearQueue)
    }

    /**
     * Abort the current processor
     */
    fun abortPayment() {
        dispatch(PaymentProcessingAction.AbortCurrentPayment)
    }
    
    // Processor-Level Input Handling
    
    /**
     * Confirm customer receipt printing (processor-level input request)
     */
    fun confirmCustomerReceiptPrinting(requestId: String, shouldPrint: Boolean) {
        dispatch(PaymentProcessingAction.ConfirmCustomerReceiptPrinting(requestId, shouldPrint))
    }
    
    /**
     * Confirm merchant PIX key (processor-level input request)
     */
    fun confirmMerchantPixKey(requestId: String, pixKey: String) {
        dispatch(PaymentProcessingAction.ConfirmMerchantPixKey(requestId, pixKey))
    }
    
    /**
     * Confirm merchant PIX has been paid (processor-level input request)
     */
    fun confirmMerchantPixHasBeenPaid(requestId: String, didPay: Boolean) {
        dispatch(PaymentProcessingAction.ConfirmMerchantPixHasBeenPaid(requestId, didPay))
    }
    
    // Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun <T: QueueItem> confirmProcessor(requestId: String, modifiedItem: T) {
        dispatch(PaymentProcessingAction.ConfirmProcessor(requestId, modifiedItem))
    }
    
    /**
     * Update processor type for all queued items
     * Used when toggling transactionless mode
     */
    fun toggleTransactionless(useTransactionless: Boolean) {
        dispatch(PaymentProcessingAction.ToggleTransactionless(useTransactionless))
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        dispatch(PaymentProcessingAction.SkipProcessor(requestId))
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(requestId: String) {
        dispatch(PaymentProcessingAction.SkipProcessorOnError(requestId))
    }
    
    // Error Handling
    
    /**
     * Handle a failed payment with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    private fun handleFailedPayment(requestId: String, action: ErrorHandlingAction) {
        dispatch(PaymentProcessingAction.HandleFailedPayment(requestId, action))
    }
    
    /**
     * Retry a failed payment immediately (queue-level input request)
     * This will retry the same processor without moving the item
     */
    fun retryPayment(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.RETRY)
    }
    
    /**
     * Retry a failed payment later (queue-level input request)
     * This will move the item to the end of the queue and continue with the next item
     */
    fun skipPayment(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.SKIP)
    }
    
    /**
     * Abort all processors and stop processing (queue-level input request)
     */
    fun abortAllPayments(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.ABORT_ALL)
    }
}
