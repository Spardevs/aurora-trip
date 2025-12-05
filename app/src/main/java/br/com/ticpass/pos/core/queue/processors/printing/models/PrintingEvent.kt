package br.com.ticpass.pos.core.queue.processors.printing.models

import br.com.ticpass.pos.core.queue.core.BaseProcessingEvent

/**
 * Printing-specific events emitted during printing processing
 */
sealed class PrintingEvent : BaseProcessingEvent {
    /**
     * Printing processing has started.
     */
    object START : PrintingEvent()

    /**
     * Printing process was canceled by user or system.
     */
    object CANCELLED : PrintingEvent()

    /**
     * Printing is being processed.
     */
    object PROCESSING : PrintingEvent()

    /**
     * Printing is currently in progress.
     */
    object PRINTING : PrintingEvent()
}
