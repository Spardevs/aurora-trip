package br.com.ticpass.pos.queue.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Payment Queue ViewModel
 * Example ViewModel for integrating the payment queue with UI
 */
class ProcessingPaymentQueueViewModel(
    private val processingPaymentStorage: ProcessingPaymentStorage
) : ViewModel() {
    
    private val queueFactory = ProcessingPaymentQueueFactory()
    
    // Create payment queue with immediate persistence (critical payment data)
    private val paymentQueue = queueFactory.createPaymentQueue(
        storage = processingPaymentStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope = viewModelScope
    )
    
    // Expose queue states to UI
    val paymentQueueState = paymentQueue.queueState
    val paymentProcessingState = paymentQueue.processingState
    
    // Expose payment processor events
    val processingPaymentEvents: SharedFlow<ProcessingPaymentEvent> = paymentQueue.processorEvents
    
    /**
     * Add a payment to the queue
     */
    fun addPayment(amount: Double, currency: String, recipientId: String, description: String) {
        viewModelScope.launch {
            val payment = ProcessingPaymentQueueItem(
                amount = amount,
                currency = currency,
                recipientId = recipientId,
                description = description,
                priority = 1
            )
            paymentQueue.enqueue(payment)
        }
    }
    
    /**
     * Get the current payment being processed, if any
     */
    fun getCurrentPayment(): ProcessingPaymentQueueItem? {
        val state = paymentProcessingState.value
        return when (state) {
            is ProcessingState.Processing -> state.item
            is ProcessingState.Retrying -> state.item
            else -> null
        }
    }
    
    /**
     * Force persist all queued items (useful for testing)
     */
    suspend fun forcePersistAll() {
        paymentQueue.forcePersist()
    }
    
    /**
     * Remove a payment from the queue
     */
    fun removePayment(payment: ProcessingPaymentQueueItem) {
        viewModelScope.launch {
            paymentQueue.remove(payment)
        }
    }
    
    /**
     * Clear all completed payments from storage
     */
    fun clearCompletedPayments() {
        viewModelScope.launch {
            paymentQueue.clearCompleted()
        }
    }
}
