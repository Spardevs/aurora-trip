package br.com.ticpass.pos.queue.processors.refund

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing PagSeguro SDK
 */
class AcquirerRefundException(
    private val errorCode: String?,
    private val result: Int?
) : Exception(errorCode) {

    /**
     * Translates the error code to a ProcessingErrorEvent
     */
    val event: ProcessingErrorEvent
        get() = AcquirerRefundErrorEvent.translate(errorCode, result.toString())
}