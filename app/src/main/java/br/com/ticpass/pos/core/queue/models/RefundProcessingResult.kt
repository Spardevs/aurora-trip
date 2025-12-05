package br.com.ticpass.pos.core.queue.models

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Processing Result
 * Represents the possible outcomes when processing a refund item
 */
class RefundSuccess : ProcessingResult.Success()
class RefundError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)