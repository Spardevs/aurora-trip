package br.com.ticpass.pos.queue.processors.printing.exceptions

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing a printing
 */
class PrintingException(
    val error: ProcessingErrorEvent
) : Exception(error.toString())