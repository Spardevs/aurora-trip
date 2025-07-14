package br.com.ticpass.pos.queue.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
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
class InteractivePaymentViewModel(
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
        data class CollectingPin(val requestId: String, val paymentId: String) : UiState()
        data class CollectingSignature(val requestId: String, val paymentId: String) : UiState()
        data class ShowingConfirmation(val requestId: String, val paymentId: String, val message: String) : UiState()
        data class ShowingOptions(val requestId: String, val paymentId: String, val prompt: String, val options: List<String>) : UiState()
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
                    else -> { /* No UI state change for other processing states */ }
                }
            }
        }
        
        // Listen for input requests from the processor
        viewModelScope.launch {
            (paymentQueue.processor.inputRequests).collectLatest { request ->
                when (request) {
                    is InputRequest.PinInput -> {
                        _uiState.value = UiState.CollectingPin(request.id, request.paymentId)
                    }
                    is InputRequest.SignatureInput -> {
                        _uiState.value = UiState.CollectingSignature(request.id, request.paymentId)
                    }
                    is InputRequest.ConfirmationInput -> {
                        _uiState.value = UiState.ShowingConfirmation(request.id, request.paymentId, request.message)
                    }
                    is InputRequest.SelectionInput -> {
                        _uiState.value = UiState.ShowingOptions(request.id, request.paymentId, request.prompt, request.options)
                    }
                }
            }
        }
    }
    
    /**
     * Process a payment with the specified processor type
     */
    fun processPayment(
        amount: Double,
        processorType: String = "acquirer", // "acquirer", "cash", or "transactionless"
        currency: String = "BRL",
        recipientId: String = "store123",
        description: String = "Payment"
    ) {
        viewModelScope.launch {
            val paymentItem = ProcessingPaymentQueueItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                currency = currency,
                recipientId = recipientId,
                description = description,
                priority = 10,
                processorType = processorType
            )
            paymentQueue.enqueue(paymentItem)
        }
    }
    
    /**
     * Submit a PIN response to the processor
     */
    fun submitPin(requestId: String, paymentId: String, pin: String) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse(requestId, paymentId, pin)
            )
            // UI will be updated via the input request flow
        }
    }
    
    /**
     * Submit a signature response to the processor
     */
    fun submitSignature(requestId: String, paymentId: String, signatureData: ByteArray) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse(requestId, paymentId, signatureData)
            )
            // UI will be updated via the input request flow
        }
    }
    
    /**
     * Submit a confirmation response to the processor
     */
    fun submitConfirmation(requestId: String, paymentId: String, confirmed: Boolean) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse(requestId, paymentId, confirmed)
            )
            // UI will be updated via the input request flow
        }
    }
    
    /**
     * Submit a selection response to the processor
     */
    fun submitSelection(requestId: String, paymentId: String, selectedIndex: Int) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse(requestId, paymentId, selectedIndex)
            )
            // UI will be updated via the input request flow
        }
    }
    
    /**
     * Cancel the current input request
     */
    fun cancelInput(requestId: String, paymentId: String) {
        viewModelScope.launch {
            paymentQueue.processor.provideInput(
                InputResponse.canceled(requestId, paymentId)
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
