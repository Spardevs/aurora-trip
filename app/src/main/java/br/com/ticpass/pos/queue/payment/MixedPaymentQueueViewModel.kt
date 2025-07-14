package br.com.ticpass.pos.queue.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Mixed Payment Queue ViewModel
 * Example ViewModel that demonstrates using a single queue for multiple payment types
 */
class MixedPaymentQueueViewModel(
    private val processingPaymentStorage: ProcessingPaymentStorage
) : ViewModel() {
    
    private val queueFactory = ProcessingPaymentQueueFactory()
    
    // Create a single queue with a dynamic processor that can handle multiple payment types
    private val paymentQueue = queueFactory.createDynamicPaymentQueue(
        storage = processingPaymentStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope = viewModelScope
    )
    
    // Expose queue states to UI
    val queueState = paymentQueue.queueState
    val processingState = paymentQueue.processingState
    val processingPaymentEvents: SharedFlow<ProcessingPaymentEvent> = paymentQueue.processorEvents
    
    /**
     * Process a acquirer card payment (using acquirer SDK)
     */
    fun processCardPayment(
        amount: Double,
        currency: String = "BRL",
        recipientId: String,
        description: String
    ) {
        viewModelScope.launch {
            val paymentItem = ProcessingPaymentQueueItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                currency = currency,
                recipientId = recipientId,
                description = description,
                priority = 10, // High priority
                processorType = "acquirer" // Uses acquirer processor
            )
            paymentQueue.enqueue(paymentItem)
        }
    }
    
    /**
     * Process a cash payment (no acquirer SDK)
     */
    fun processCashPayment(
        amount: Double,
        currency: String = "BRL",
        recipientId: String,
        description: String
    ) {
        viewModelScope.launch {
            val paymentItem = ProcessingPaymentQueueItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                currency = currency,
                recipientId = recipientId,
                description = description,
                priority = 5, // Medium priority
                processorType = "cash" // Uses cash processor
            )
            paymentQueue.enqueue(paymentItem)
        }
    }
    
    /**
     * Process a demo payment (always succeeds, for testing)
     */
    fun processDemoPayment(
        amount: Double,
        currency: String = "BRL",
        recipientId: String,
        description: String
    ) {
        viewModelScope.launch {
            val paymentItem = ProcessingPaymentQueueItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                currency = currency,
                recipientId = recipientId,
                description = description,
                priority = 1, // Low priority
                processorType = "transactionless" // Uses transactionless processor
            )
            paymentQueue.enqueue(paymentItem)
        }
    }
    
    /**
     * Example of enqueuing multiple payment types at once
     * All payments are processed sequentially in the same queue based on priority
     */
    fun processMultiplePayments() {
        viewModelScope.launch {
            // Enqueue two acquirer card payments
            paymentQueue.enqueue(ProcessingPaymentQueueItem(
                amount = 75.0,
                currency = "BRL",
                recipientId = "store123",
                description = "First card payment",
                processorType = "acquirer",
                priority = 10 // High priority
            ))
            
            paymentQueue.enqueue(ProcessingPaymentQueueItem(
                amount = 120.0,
                currency = "BRL",
                recipientId = "store123",
                description = "Second card payment",
                processorType = "acquirer",
                priority = 5 // Medium priority
            ))
            
            // Enqueue a cash payment - will be processed based on priority
            paymentQueue.enqueue(ProcessingPaymentQueueItem(
                amount = 50.0,
                currency = "BRL",
                recipientId = "store123",
                description = "Cash payment",
                processorType = "cash",
                priority = 8 // Medium-high priority - will be processed after the first card payment
            ))
        }
    }
    
    /**
     * Cancel a payment
     */
    fun cancelPayment(paymentItem: ProcessingPaymentQueueItem) {
        viewModelScope.launch {
            paymentQueue.remove(paymentItem)
        }
    }
    
    /**
     * Get the current payment being processed, if any
     */
    fun getCurrentPayment(): ProcessingPaymentQueueItem? {
        val state = processingState.value
        return when (state) {
            is ProcessingState.Processing -> state.item
            is ProcessingState.Retrying -> state.item
            else -> null
        }
    }
}
