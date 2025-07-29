package br.com.ticpass.pos.queue.processors.payment.exceptions

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing a payment
 */
class PaymentProcessingException(
    private val error: ProcessingErrorEvent
) : Exception(error.toString())