package br.com.ticpass.pos.queue.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingState
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
        data class confirmCustomerReceiptPrinting(val requestId: String, val doPrint: Boolean) : UiState()
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // Observe processing state
        viewModelScope.launch {
            processingState.collectLatest { state ->
                when (state) {
                    is ProcessingState.Processing -> _uiState.value = UiState.Processing
                    is ProcessingState.Idle -> _uiState.value = UiState.Idle
                    is ProcessingState.Failed -> {
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
                        _uiState.value = UiState.confirmCustomerReceiptPrinting(
                            requestId = request.id,
                            doPrint = false // Default value, user can change via UI
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
     * Cancel the current input request
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
     * Cancel the current input request
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
