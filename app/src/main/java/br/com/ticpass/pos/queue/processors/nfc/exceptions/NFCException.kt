package br.com.ticpass.pos.queue.processors.nfc.exceptions

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing a nfc
 */
class NFCException(
    val error: ProcessingErrorEvent
) : Exception(error.toString())