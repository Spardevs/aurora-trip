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
    
    /**
     * Single entry point for UI state updates
     * All state changes should go through this method to ensure consistency
     */
    private fun updateState(newState: UiState) {
        _uiState.value = newState
    }
    
    //endregion
    
    //region Initialization and Event Handling
    
    init {
        // Observe processing state
        launchInViewModelScope {
            processingState.collectLatest { state ->
                when (state) {
                    is ProcessingState.ItemProcessing -> updateState(UiState.Processing)
                    is ProcessingState.QueueIdle -> updateState(UiState.Idle)
                    is ProcessingState.ItemFailed -> updateState(UiState.Error(state.error))
                    else -> { /* No UI state change for other processing states */ }
                }
            }
        }
        
        // Listen for input requests from the processor
        launchInViewModelScope {
            (paymentQueue.processor.inputRequests).collectLatest { request ->
                when (request) {
                    is InputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
                        updateState(UiState.ConfirmCustomerReceiptPrinting(
                            requestId = request.id,
                            doPrint = false // Default value, user can change via UI
                        ))
                    }
                }
            }
        }
        
        // Listen for queue-level input requests
        launchInViewModelScope {
            paymentQueue.queueInputRequests.collectLatest { request ->
                when (request) {
                    is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                        updateState(UiState.ConfirmNextProcessor(
                            requestId = request.id,
                            currentItemIndex = request.currentItemIndex,
                            totalItems = request.totalItems
                        ))
                    }
                    is PaymentQueueInputRequest.CONFIRM_NEXT_PAYMENT -> {
                        updateState(UiState.ConfirmNextPaymentProcessor(
                            requestId = request.id,
                            currentItemIndex = request.currentItemIndex,
                            totalItems = request.totalItems,
                            currentAmount = request.currentAmount,
                            currentMethod = request.currentMethod,
                            currentProcessorType = request.currentProcessorType
                        ))
                    }
                    is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                        updateState(UiState.ErrorRetryOrSkip(
                            requestId = request.id,
                            error = request.error
                        ))
                    }
                }
            }
        }
    }
    
    //endregion

    //region Queue Operations
    
    /**
     * Start processing the payment queue
     */
    fun startProcessing() {
        launchInViewModelScope {
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
        launchInViewModelScope {
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
     * Cancel a payment
     */
    fun cancelPayment(paymentId: String) {
        launchInViewModelScope {
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
        launchInViewModelScope {
            paymentQueue.removeAll()
        }
    }
    
    //endregion

    //region Processor-Level Input Handling
    
    /**
     * Confirm customer receipt printing (processor-level input request)
     */
    fun confirmCustomerReceiptPrinting(requestId: String, doPrint: Boolean) {
        launchInViewModelScope {
            paymentQueue.processor.provideInput(
                InputResponse(requestId, doPrint)
            )
            // Reset UI state
            updateState(UiState.Processing)
        }
    }

    /**
     * Cancel the current processor-level input request
     */
    fun cancelInput(requestId: String) {
        launchInViewModelScope {
            paymentQueue.processor.provideInput(
                InputResponse.canceled(requestId)
            )
            // UI will be updated via the input request flow
        }
    }
    
    //endregion
    
    //region Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun confirmNextProcessor(requestId: String) {
        launchInViewModelScope {
            val response = QueueInputResponse.proceed(requestId)
            paymentQueue.provideQueueInput(response)
            updateState(UiState.Processing)
        }
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
        launchInViewModelScope {
            val response = PaymentQueueInputResponse.proceedWithModifiedPayment(
                requestId = requestId,
                modifiedAmount = modifiedAmount,
                modifiedMethod = modifiedMethod,
                modifiedProcessorType = modifiedProcessorType
            )
            paymentQueue.provideQueueInput(response)
            // Reset UI state
            updateState(UiState.Processing)
        }
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        launchInViewModelScope {
            val response = QueueInputResponse.skip(requestId)
            paymentQueue.provideQueueInput(response)
            // UI will be updated via the queue input request flow
        }
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
        launchInViewModelScope {
            val response = when (action) {
                ErrorHandlingAction.RETRY_IMMEDIATELY -> QueueInputResponse.retryImmediately(requestId)
                ErrorHandlingAction.RETRY_LATER -> QueueInputResponse.retryLater(requestId)
                ErrorHandlingAction.ABORT_CURRENT -> QueueInputResponse.abortCurrentProcessor(requestId)
                ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.abortAllProcessors(requestId)
            }
            paymentQueue.provideQueueInput(response)
            
            // Reset UI state for actions that continue processing
            if (action == ErrorHandlingAction.RETRY_IMMEDIATELY || action == ErrorHandlingAction.RETRY_LATER) {
                updateState(UiState.Processing)
            }
            // For other actions, UI will be updated via the queue input request flow
        }
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
    
    //endregion
}
