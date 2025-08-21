package br.com.ticpass.pos.queue.processors.nfc

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import stone.application.enums.ErrorsEnum

/**
 * Exception thrown when there is an error processing Stone SDK
 */
class AcquirerNFCException : Exception {
    private val errorEnum: ErrorsEnum?
    private val errorEvent: ProcessingErrorEvent?

    constructor(errorEnum: ErrorsEnum) : super(errorEnum.toString()) {
        this.errorEnum = errorEnum
        this.errorEvent = null
    }

    constructor(errorEvent: ProcessingErrorEvent) : super(errorEvent.toString()) {
        this.errorEnum = null
        this.errorEvent = errorEvent
    }

    /**
     * Translates the error to a ProcessingErrorEvent
     */
    val event: ProcessingErrorEvent
        get() {
            return errorEvent ?: AcquirerNFCErrorEvent.translate(errorEnum!!)
        }
}