package br.com.ticpass.pos.core.queue.processors.refund

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Gertec proprietary refund error events (NO-OP)
 * 
 * This variant does not support refund operations.
 * Minimal stub for compilation compatibility.
 */
enum class AcquirerRefundErrorEvent(val code: String, val event: ProcessingErrorEvent) {
    
    UNSUPPORTED_OPERATION(
        "UNSUPPORTED_OPERATION",
        ProcessingErrorEvent.GENERIC
    ),
    
    GENERIC_ERROR(
        "GENERIC_ERROR",
        ProcessingErrorEvent.GENERIC
    );

    companion object {
        fun translate(code: String): ProcessingErrorEvent {
            return entries.find { it.code.equals(code, ignoreCase = true) }?.event 
                ?: ProcessingErrorEvent.GENERIC
        }

        fun translate(event: ProcessingErrorEvent): String? {
            return entries.find { it.event == event }?.code
        }
    }
}
