package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing a payment
 */
class PaymentProcessingException(
    private val error: ProcessingErrorEvent
) : Exception(error.toString())