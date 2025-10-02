package br.com.ticpass.pos.queue.processors.nfc

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Exception thrown when there is an error processing NFC operations (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
class AcquirerNFCException : Exception {
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
            return errorEvent ?: AcquirerNFCErrorEvent.translate(errorCode!!)
        }
}
