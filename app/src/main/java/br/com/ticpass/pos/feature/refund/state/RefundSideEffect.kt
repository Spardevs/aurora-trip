package br.com.ticpass.pos.feature.refund.state

/**
 * Represents a side effect that should be executed by the ViewModel
 * Side effects are operations that don't directly affect the UI state,
 * such as network calls, database operations, etc.
 */
sealed class RefundSideEffect {
    /**
     * The operation to execute as part of this side effect
     */
    abstract val scope: suspend () -> Unit
    
    /**
     * Start processing the refund queue
     */
    data class StartProcessingQueue(override val scope: suspend () -> Unit) : RefundSideEffect()
    
    /**
     * Enqueue a refund item
     */
    data class EnqueueRefundItem(override val scope: suspend () -> Unit) : RefundSideEffect()
    
    /**
     * Remove a refund item
     */
    data class RemoveRefundItem(override val scope: suspend () -> Unit) : RefundSideEffect()
    
    /**
     * Remove all refund items
     */
    data class ClearRefundQueue(override val scope: suspend () -> Unit) : RefundSideEffect()

    /**
     * Abort the current refund processing
     */
    data class AbortCurrentRefund( override val scope: suspend () -> Unit) : RefundSideEffect()
    
    /**
     * Provide input to a processor
     */
    data class ProvideProcessorInput(override val scope: suspend () -> Unit) : RefundSideEffect()
    
    /**
     * Provide input to the queue
     */
    data class ProvideQueueInput(override val scope: suspend () -> Unit) : RefundSideEffect()
}
