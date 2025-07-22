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
    private val processingPaymentStorage: ProcessingPaymentStorage
) : ViewModel() {
    
    private val queueFactory = ProcessingPaymentQueueFactory()
    
    // Create a payment queue with a dynamic processor
    private val paymentQueue = queueFactory.createDynamicPaymentQueue(
        storage = processingPaymentStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        queueConfirmationMode = QueueConfirmationMode.CONFIRMATION, // Enable confirmation between processors
        scope = viewModelScope
    )
    
    // Expose queue states to UI
    val queueState = paymentQueue.queueState
    val processingState = paymentQueue.processingState
    val processingPaymentEvents: SharedFlow<ProcessingPaymentEvent> = paymentQueue.processorEvents
    
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
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // Observe processing state
        viewModelScope.launch {
            processingState.collectLatest { state ->
                when (state) {
                    is ProcessingState.ItemProcessing -> _uiState.value = UiState.Processing
                    is ProcessingState.QueueIdle -> _uiState.value = UiState.Idle
                    is ProcessingState.ItemFailed -> {
                        _uiState.value = UiState.Error(state.error)
                    }
                    else -> { /* No UI state change for other processing states */ }
                }
            }
        }
        
        // Listen for input requests from the processor
        viewModelScope.launch {
            (paymentQueue.processor.inputRequests).collectLatest { request ->
                when (request) {
                    is InputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
                        _uiState.value = UiState.ConfirmCustomerReceiptPrinting(
                            requestId = request.id,
                            doPrint = false // Default value, user can change via UI
                        )
                    }
                }
            }
        }
        
        // Listen for queue-level input requests
        viewModelScope.launch {
            paymentQueue.queueInputRequests.collectLatest { request ->
                when (request) {
                    is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                        _uiState.value = UiState.ConfirmNextProcessor(
                            requestId = request.id,
                            currentItemIndex = request.currentItemIndex,
                            totalItems = request.totalItems
                        )
                    }
                    is PaymentQueueInputRequest.CONFIRM_NEXT_PAYMENT -> {
                        _uiState.value = UiState.ConfirmNextPaymentProcessor(
                            requestId = request.id,
                            currentItemIndex = request.currentItemIndex,
                            totalItems = request.totalItems,
                            currentAmount = request.currentAmount,
                            currentMethod = request.currentMethod,
                            currentProcessorType = request.currentProcessorType
                        )
                    }
                    is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                        _uiState.value = UiState.ErrorRetryOrSkip(
                            requestId = request.id,
                            error = request.error
                        )
                    }
                }
            }
        }
    }

    fun startProcessing() {
        viewModelScope.launch {
            paymentQueue.startProcessing()
        }
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
        viewModelScope.launch {
            val paymentItem = ProcessingPaymentQueueItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                commission = commission,
                method = method,
                priority = 10,
                processorType = processorType
            )
            paymentQueue.enqueue(paymentItem)
        }
    }

    /**
     * Confirm customer receipt printing (processor-level input request)
     */
    fun confirmCustomerReceiptPrinting(requestId: String, doPrint: Boolean) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse(requestId, doPrint)
            )
        }
        // Reset UI state
        _uiState.value = UiState.Processing
    }

    /**
     * Cancel the current processor-level input request
     */
    fun cancelInput(requestId: String) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse.canceled(requestId)
            )
            // UI will be updated via the input request flow
        }
    }
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun confirmNextProcessor(requestId: String) {
        viewModelScope.launch {
            val response = QueueInputResponse.proceed(requestId)
            paymentQueue.provideQueueInput(response)
        }
        // Reset UI state
        _uiState.value = UiState.Processing
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
        viewModelScope.launch {
            val response = PaymentQueueInputResponse.proceedWithModifiedPayment(
                requestId = requestId,
                modifiedAmount = modifiedAmount,
                modifiedMethod = modifiedMethod,
                modifiedProcessorType = modifiedProcessorType
            )
            paymentQueue.provideQueueInput(response)
        }
        // Reset UI state
        _uiState.value = UiState.Processing
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        viewModelScope.launch {
            val response = QueueInputResponse.skip(requestId)
            paymentQueue.provideQueueInput(response)
        }
        // UI will be updated via the queue input request flow
    }
    
    /**
     * Handle a failed payment with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    fun handleFailedPayment(requestId: String, action: ErrorHandlingAction) {
        viewModelScope.launch {
            val response = when (action) {
                ErrorHandlingAction.RETRY_IMMEDIATELY -> QueueInputResponse.retryImmediately(requestId)
                ErrorHandlingAction.RETRY_LATER -> QueueInputResponse.retryLater(requestId)
                ErrorHandlingAction.ABORT_CURRENT -> QueueInputResponse.abortCurrentProcessor(requestId)
                ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.abortAllProcessors(requestId)
            }
            paymentQueue.provideQueueInput(response)
        }
        
        // Reset UI state for actions that continue processing
        if (action == ErrorHandlingAction.RETRY_IMMEDIATELY || action == ErrorHandlingAction.RETRY_LATER) {
            _uiState.value = UiState.Processing
        }
        // For other actions, UI will be updated via the queue input request flow
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
     * This will move the item to the end of the queue for later retry
     */
    fun retryFailedPaymentLater(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.RETRY_LATER)
    }
    
    /**
     * Abort the current processor but keep the item in queue (queue-level input request)
     */
    fun abortCurrentProcessor(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.ABORT_CURRENT)
    }
    
    /**
     * Abort all processors and cancel queue processing (queue-level input request)
     */
    fun abortAllProcessors(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.ABORT_ALL)
    }
    
    /**
     * Cancel a payment
     */
    fun cancelPayment(paymentId: String) {
        viewModelScope.launch {
            val item = paymentQueue.queueState.value.find { it.id == paymentId }
            if (item != null) {
                paymentQueue.remove(item)
            }
        }
    }
    
    /**
     * Cancel all payments
     * Uses a single operation to remove all items at once
     */
    fun cancelAllPayments() {
        viewModelScope.launch {
            // Remove all items at once (more efficient)
            paymentQueue.removeAll()
        }
    }
}
