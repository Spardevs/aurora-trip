package br.com.ticpass.pos.queue.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Multi-Payment Queue ViewModel
 * Example ViewModel for integrating multiple payment processors based on payment method
 */
class MultiPaymentQueueViewModel(
    private val processingPaymentStorage: ProcessingPaymentStorage
) : ViewModel() {
    
    private val queueFactory = ProcessingPaymentQueueFactory()
    
    // Create different payment queues with different processors
    private val acquirerQueue = queueFactory.createPaymentQueue(
        storage = processingPaymentStorage,
        paymentMethod = "acquirer", // Uses acquirer SDK
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope = viewModelScope
    )
    
    private val cashQueue = queueFactory.createPaymentQueue(
        storage = processingPaymentStorage,
        paymentMethod = "cash", // No acquirer SDK
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope = viewModelScope
    )
    
    private val demoQueue = queueFactory.createPaymentQueue(
        storage = processingPaymentStorage,
        paymentMethod = "transactionless", // Just simulates payment
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope = viewModelScope
    )
    
    // Active queue reference (changes based on selected payment method)
    private var activeQueue = acquirerQueue
    
    // Current payment method
    private val _currentPaymentMethod = MutableStateFlow<PaymentMethod>(PaymentMethod.ACQUIRER)
    val currentPaymentMethod: StateFlow<PaymentMethod> = _currentPaymentMethod.asStateFlow()
    
    // Expose active queue states
    val queueState get() = activeQueue.queueState
    val processingState get() = activeQueue.processingState
    val processingPaymentEvents: SharedFlow<ProcessingPaymentEvent> get() = activeQueue.processorEvents
    
    /**
     * Set payment method and switch to appropriate queue
     */
    fun setPaymentMethod(method: PaymentMethod) {
        _currentPaymentMethod.value = method
        activeQueue = when (method) {
            PaymentMethod.ACQUIRER -> acquirerQueue
            PaymentMethod.CASH -> cashQueue
            PaymentMethod.DEMO -> demoQueue
        }
    }
    
    /**
     * Process a payment with the current active queue
     */
    fun processPayment(
        amount: Int,
        commission: Int,
        method: SystemPaymentMethod,
    ) {
        viewModelScope.launch {
            val paymentItem = ProcessingPaymentQueueItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                commission = commission,
                method = method,
                priority = 10 // High priority for payments
            )
            activeQueue.enqueue(paymentItem)
        }
    }
    
    /**
     * Cancel a payment
     */
    fun cancelPayment(paymentItem: ProcessingPaymentQueueItem) {
        viewModelScope.launch {
            // Cancel in all queues to ensure it's removed
            // regardless of which queue it was added to
            acquirerQueue.remove(paymentItem)
            cashQueue.remove(paymentItem)
            demoQueue.remove(paymentItem)
        }
    }
    
    /**
     * Get the current payment being processed, if any
     */
    fun getCurrentPayment(): ProcessingPaymentQueueItem? {
        val state = processingState.value
        return when (state) {
            is ProcessingState.ItemProcessing -> state.item
            is ProcessingState.ItemRetrying -> state.item
            else -> null
        }
    }
    
    /**
     * Clear all completed payments from storage
     */
    fun clearCompletedPayments() {
        viewModelScope.launch {
            // Only need to call on one queue since they share storage
            activeQueue.clearCompleted()
        }
    }
    
    /**
     * Payment methods supported by the ViewModel
     */
    enum class PaymentMethod {
        ACQUIRER, // Uses acquirer SDK
        CASH,     // Cash payment (no card)
        DEMO      // For demonstrations, always succeeds
    }
}
