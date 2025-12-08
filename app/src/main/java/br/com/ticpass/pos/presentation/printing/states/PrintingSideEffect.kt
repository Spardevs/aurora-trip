package br.com.ticpass.pos.presentation.printing.states

/**
 * Represents a side effect that should be executed by the ViewModel
 * Side effects are operations that don't directly affect the UI state,
 * such as network calls, database operations, etc.
 */
sealed class PrintingSideEffect {
    /**
     * The operation to execute as part of this side effect
     */
    abstract val scope: suspend () -> Unit
    
    /**
     * Start processing the printing queue
     */
    data class StartProcessingQueue(override val scope: suspend () -> Unit) : PrintingSideEffect()
    
    /**
     * Enqueue a printing item
     */
    data class EnqueuePrintingItem(override val scope: suspend () -> Unit) : PrintingSideEffect()
    
    /**
     * Remove a printing item
     */
    data class RemovePrintingItem(override val scope: suspend () -> Unit) : PrintingSideEffect()
    
    /**
     * Remove all printing items
     */
    data class ClearPrintingQueue(override val scope: suspend () -> Unit) : PrintingSideEffect()

    /**
     * Abort the current printing processing
     */
    data class AbortCurrentPrinting( override val scope: suspend () -> Unit) : PrintingSideEffect()
    
    /**
     * Provide input to a processor
     */
    data class ProvideProcessorInput(override val scope: suspend () -> Unit) : PrintingSideEffect()
    
    /**
     * Provide input to the queue
     */
    data class ProvideQueueInput(override val scope: suspend () -> Unit) : PrintingSideEffect()
}
