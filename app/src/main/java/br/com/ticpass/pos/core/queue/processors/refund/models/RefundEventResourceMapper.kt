package br.com.ticpass.pos.core.queue.processors.refund.models

import br.com.ticpass.pos.R

/**
 * Maps RefundEvent types to string resource keys
 * This centralized mapping makes it easy to maintain error message mappings
 */
object RefundEventResourceMapper {
    
    /**
     * Get the string resource key for a RefundEvent
     * @param event The RefundEvent to map
     * @return The string resource key corresponding to the event
     */
    fun getErrorResourceKey(event: RefundEvent): Int {
        return when (event) {
            is RefundEvent.START -> R.string.event_start
            RefundEvent.CANCELLED -> R.string.event_cancelled
            RefundEvent.PROCESSING -> R.string.event_refund_processing
            RefundEvent.REFUNDING -> R.string.event_refund
            RefundEvent.SUCCESS -> R.string.event_refund_success
        }
    }
}
