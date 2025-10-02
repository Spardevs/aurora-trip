package br.com.ticpass.pos.queue.processors.nfc

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Gertec proprietary NFC error events (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
enum class AcquirerNFCErrorEvent(val code: String, val event: ProcessingErrorEvent) {
    
    UNSUPPORTED_OPERATION(
        "UNSUPPORTED_OPERATION",
        ProcessingErrorEvent.GENERIC
    ),
    
    GENERIC_ERROR(
        "GENERIC_ERROR",
        ProcessingErrorEvent.GENERIC
    );

    companion object {
        /**
         * Translates an error code to a ProcessingErrorEvent.
         * @param code The error code string
         * @return The corresponding ProcessingErrorEvent.
         */
        fun translate(code: String): ProcessingErrorEvent {
            return entries.find { it.code.equals(code, ignoreCase = true) }?.event 
                ?: ProcessingErrorEvent.GENERIC
        }

        /**
         * Translates a ProcessingErrorEvent to its corresponding error code.
         * @param event The ProcessingErrorEvent to translate.
         * @return The corresponding error code string.
         */
        fun translate(event: ProcessingErrorEvent): String? {
            return entries.find { it.event == event }?.code
        }
    }
}
