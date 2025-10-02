package br.com.ticpass.pos.queue.processors.refund

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing refund operations (NO-OP)
 * 
 * This variant does not support refund operations.
 * Minimal stub for compilation compatibility.
 */
class AcquirerRefundException(
    val event: ProcessingErrorEvent
) : Exception(event.toString())
