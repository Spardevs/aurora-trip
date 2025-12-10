package br.com.ticpass.pos.core.queue.models

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Processing Result
 * Represents the possible outcomes when processing a payment item
 */
class PaymentSuccess(
    val atk: String,
    val txId: String,
) : ProcessingResult.Success()

class PaymentError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)