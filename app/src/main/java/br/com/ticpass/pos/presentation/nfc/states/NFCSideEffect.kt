package br.com.ticpass.pos.presentation.nfc.states

/**
 * Represents a side effect that should be executed by the ViewModel
 * Side effects are operations that don't directly affect the UI state,
 * such as network calls, database operations, etc.
 */
sealed class NFCSideEffect {
    /**
     * The operation to execute as part of this side effect
     */
    abstract val scope: suspend () -> Unit
    
    /**
     * Start processing the nfc queue
     */
    data class StartProcessingQueue(override val scope: suspend () -> Unit) : NFCSideEffect()
    
    /**
     * Enqueue a nfc item
     */
    data class EnqueueNFCItem(override val scope: suspend () -> Unit) : NFCSideEffect()
    
    /**
     * Remove a nfc item
     */
    data class RemoveNFCItem(override val scope: suspend () -> Unit) : NFCSideEffect()
    
    /**
     * Remove all nfc items
     */
    data class ClearNFCQueue(override val scope: suspend () -> Unit) : NFCSideEffect()

    /**
     * Abort the current nfc processing
     */
    data class AbortCurrentNFC( override val scope: suspend () -> Unit) : NFCSideEffect()
    
    /**
     * Provide input to a processor
     */
    data class ProvideProcessorInput(override val scope: suspend () -> Unit) : NFCSideEffect()
    
    /**
     * Provide input to the queue
     */
    data class ProvideQueueInput(override val scope: suspend () -> Unit) : NFCSideEffect()
}
