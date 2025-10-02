package br.com.ticpass.pos.queue.processors.printing

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing Gertec SK210 SDK
 */
class AcquirerPrintingException : Exception {
    private val errorCode: String?
    private val errorEvent: ProcessingErrorEvent?

    constructor(errorCode: String) : super(errorCode) {
        this.errorCode = errorCode
        this.errorEvent = null
    }

    constructor(errorEvent: ProcessingErrorEvent) : super(errorEvent.toString()) {
        this.errorCode = null
        this.errorEvent = errorEvent
    }

    /**
     * Translates the error to a ProcessingErrorEvent
     */
    val event: ProcessingErrorEvent
        get() {
            return errorEvent ?: AcquirerPrintingErrorEvent.translate(errorCode!!)
        }
}
