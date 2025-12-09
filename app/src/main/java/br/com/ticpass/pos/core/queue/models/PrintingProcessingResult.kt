package br.com.ticpass.pos.core.queue.models

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Processing Result
 * Represents the possible outcomes when processing a printing item
 */
class PrintingSuccess : ProcessingResult.Success()
class PrintingError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)