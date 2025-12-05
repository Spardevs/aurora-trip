package br.com.ticpass.pos.core.queue.processors.printing.models

import br.com.ticpass.pos.R

/**
 * Maps PrintingEvent types to string resource keys
 * This centralized mapping makes it easy to maintain error message mappings
 */
object PrintingEventResourceMapper {
    
    /**
     * Get the string resource key for a PrintingEvent
     * @param event The PrintingEvent to map
     * @return The string resource key corresponding to the event
     */
    fun getErrorResourceKey(event: PrintingEvent): Int {
        return when (event) {
            is PrintingEvent.START -> R.string.event_start
            PrintingEvent.CANCELLED -> R.string.event_cancelled
            PrintingEvent.PROCESSING -> R.string.event_printing_processing
            PrintingEvent.PRINTING -> R.string.event_printing
        }
    }
}
