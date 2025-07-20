package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.ProcessingErrorEvent

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