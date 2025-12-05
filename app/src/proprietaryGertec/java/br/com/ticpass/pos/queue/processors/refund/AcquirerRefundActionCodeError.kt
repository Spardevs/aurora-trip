package br.com.ticpass.pos.core.queue.processors.refund

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Refund action code error translator (NO-OP)
 * 
 * This variant does not support refund operations.
 * Minimal stub for compilation compatibility.
 */
object AcquirerRefundActionCodeError {
    
    fun translate(actionCode: AcquirerRefundActionCode): ProcessingErrorEvent {
        return ProcessingErrorEvent.GENERIC
    }
}
