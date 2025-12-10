package br.com.ticpass.pos.core.queue.processors.refund.models

import br.com.ticpass.pos.core.queue.core.BaseProcessingEvent

/**
 * Refund-specific events emitted during refund processing
 */
sealed class RefundEvent : BaseProcessingEvent {
    /**
     * Refund processing has started.
     */
    object START : RefundEvent()

    /**
     * Refund process was canceled by user or system.
     */
    object CANCELLED : RefundEvent()

    /**
     * Refund is being processed.
     */
    object PROCESSING : RefundEvent()

    /**
     * Refund is currently in progress.
     */
    object REFUNDING : RefundEvent()

    /**
     * Refund is success.
     */
    object SUCCESS : RefundEvent()
}
