package br.com.ticpass.pos.core.queue.processors.refund.exceptions

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing a refund
 */
class RefundException(
    val error: ProcessingErrorEvent
) : Exception(error.toString())