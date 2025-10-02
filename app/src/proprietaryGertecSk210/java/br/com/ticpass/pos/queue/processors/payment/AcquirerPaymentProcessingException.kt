package br.com.ticpass.pos.queue.processors.payment

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing payment operations (NO-OP)
 * 
 * This variant does not support payment operations.
 * Minimal stub for compilation compatibility.
 */
class AcquirerPaymentProcessingException(
    val event: ProcessingErrorEvent
) : Exception(event.toString())
