package br.com.ticpass.pos.core.queue.processors.printing

import br.com.gertec.easylayer.utils.Status
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Gertec-specific error events emitted during printing processing
 * 
 * This is a minimal implementation - actual error codes will be added when implementing the logic
 */
enum class AcquirerPrintingErrorEvent(val code: String, val event: ProcessingErrorEvent) {

    OK(
        Status.OK.value.toString(),
        ProcessingErrorEvent.CONNECTION_ERROR
    ),

    OVERHEAT(
        Status.OVERHEAT.value.toString(),
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),
    
    // Printer errors
    OUT_OF_PAPER(
        Status.OUT_OF_PAPER.value.toString(),
        ProcessingErrorEvent.PRINTER_ERROR
    ),

    UNKNOWN_ERROR(
        Status.UNKNOWN_ERROR.value.toString(),
        ProcessingErrorEvent.GENERIC
    );

    companion object {
        /**
         * Translates a Gertec error code to a ProcessingErrorEvent.
         * @param code The error code from Gertec SDK.
         * @return The corresponding ProcessingErrorEvent.
         */
        fun translate(code: String): ProcessingErrorEvent {
            return entries.find { it.code == code }?.event ?: ProcessingErrorEvent.GENERIC
        }

        /**
         * Translates a ProcessingErrorEvent to its Gertec corresponding error code.
         * @param event The ProcessingErrorEvent to translate.
         * @return The corresponding error code.
         */
        fun translate(event: ProcessingErrorEvent): String? {
            return entries.find { it.event == event }?.code
        }
    }
}
