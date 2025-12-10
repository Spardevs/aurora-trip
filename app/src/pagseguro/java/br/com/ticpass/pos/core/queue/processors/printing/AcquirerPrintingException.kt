package br.com.ticpass.pos.core.queue.processors.printing

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing PagSeguro SDK
 */
class AcquirerPrintingException(
    private val errorCode: String?,
    private val result: Int?
) : Exception(errorCode) {

    /**
     * Translates the error code to a ProcessingErrorEvent
     */
    val event: ProcessingErrorEvent
        get() = AcquirerPrintingErrorEvent.translate(errorCode, result.toString())
}