package br.com.ticpass.pos.feature.payment.state

/**
 * Represents a side effect that should be executed by the ViewModel
 * Side effects are operations that don't directly affect the UI state,
 * such as network calls, database operations, etc.
 */
sealed class PaymentProcessingSideEffect {
    /**
     * The operation to execute as part of this side effect
     */
    abstract val scope: suspend () -> Unit
    
    /**
     * Start processing the payment queue
     */
    data class StartProcessingQueue(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
    
    /**
     * Enqueue a payment item
     */
    data class EnqueuePaymentItem(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
    
    /**
     * Remove a payment item
     */
    data class RemovePaymentItem(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
    
    /**
     * Remove all payment items
     */
    data class RemoveAllPaymentItems(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
    
    /**
     * Provide input to a processor
     */
    data class ProvideProcessorInput(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
    
    /**
     * Provide input to the queue
     */
    data class ProvideQueueInput(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
    
    /**
     * Update processor types for all queued items
     * Used when toggling transactionless mode
     */
    data class UpdateAllProcessorTypes(override val scope: suspend () -> Unit) : PaymentProcessingSideEffect()
}
