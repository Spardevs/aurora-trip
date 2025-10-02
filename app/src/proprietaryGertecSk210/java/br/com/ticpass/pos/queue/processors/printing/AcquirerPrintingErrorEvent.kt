package br.com.ticpass.pos.queue.processors.printing

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Gertec SK210-specific error events emitted during printing processing
 * 
 * This is a minimal implementation - actual error codes will be added when implementing the logic
 */
enum class AcquirerPrintingErrorEvent(val code: String, val event: ProcessingErrorEvent) {

    CONNECTIVITY_ERROR(
        "CONNECTIVITY_ERROR",
        ProcessingErrorEvent.CONNECTION_ERROR
    ),

    OPERATION_CANCELLED_BY_USER(
        "OPERATION_CANCELLED_BY_USER",
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),
    
    // Printer errors
    PRINTER_GENERIC_ERROR(
        "PRINTER_GENERIC_ERROR",
        ProcessingErrorEvent.PRINTER_ERROR
    ),
    
    PRINTER_BUSY_ERROR(
        "PRINTER_BUSY_ERROR",
        ProcessingErrorEvent.PRINTER_BUSY
    ),
    
    PRINTER_OUT_OF_PAPER(
        "PRINTER_OUT_OF_PAPER_ERROR",
        ProcessingErrorEvent.PRINTER_OUT_OF_PAPER
    ),
    
    PRINTER_LOW_ENERGY(
        "PRINTER_LOW_ENERGY_ERROR",
        ProcessingErrorEvent.LOW_BATTERY
    ),
    
    PRINTER_OVERHEATING(
        "PRINTER_OVERHEATING_ERROR",
        ProcessingErrorEvent.PRINTER_OVERHEATING
    ),
    
    // Generic and system errors
    GENERIC_ERROR(
        "GENERIC_ERROR",
        ProcessingErrorEvent.GENERIC
    ),
    
    UNKNOWN_ERROR(
        "UNKNOWN_ERROR",
        ProcessingErrorEvent.GENERIC
    );

    companion object {
        /**
         * Translates a Gertec SK210 error code to a ProcessingErrorEvent.
         * @param code The error code from Gertec SK210 SDK.
         * @return The corresponding ProcessingErrorEvent.
         */
        fun translate(code: String): ProcessingErrorEvent {
            return entries.find { it.code == code }?.event ?: ProcessingErrorEvent.GENERIC
        }

        /**
         * Translates a ProcessingErrorEvent to its Gertec SK210 corresponding error code.
         * @param event The ProcessingErrorEvent to translate.
         * @return The corresponding error code.
         */
        fun translate(event: ProcessingErrorEvent): String? {
            return entries.find { it.event == event }?.code
        }
    }
}
