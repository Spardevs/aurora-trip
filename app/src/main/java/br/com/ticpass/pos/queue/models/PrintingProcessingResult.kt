package br.com.ticpass.pos.queue.models

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Processing Result
 * Represents the possible outcomes when processing a printing item
 */
class PrintingSuccess : ProcessingResult.Success()
class PrintingError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)