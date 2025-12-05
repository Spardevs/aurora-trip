package br.com.ticpass.pos.queue.processors.payment

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing PagSeguro SDK
 */
class AcquirerProcessingException(
    private val errorCode: String?,
    private val result: Int?
) : Exception(errorCode) {

    /**
     * Translates the error code to a ProcessingErrorEvent
     */
    val event: ProcessingErrorEvent
        get() = AcquirerPaymentErrorEvent.translate(errorCode, result.toString())
}