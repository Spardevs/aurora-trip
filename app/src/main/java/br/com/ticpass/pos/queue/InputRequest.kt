package br.com.ticpass.pos.queue

import java.util.UUID

/**
 * Input Request
 * Represents a request for user input during processing
 */
sealed class InputRequest {
    abstract val id: String
    abstract val timeoutMs: Long?
    
    /**
     * Request for PIN entry
     */
    data class PinInput(
        override val id: String = UUID.randomUUID().toString(),
        val paymentId: String,
        override val timeoutMs: Long? = 60_000L // 1 minute timeout
    ) : InputRequest()
    
    /**
     * Request for signature capture
     */
    data class SignatureInput(
        override val id: String = UUID.randomUUID().toString(),
        val paymentId: String,
        override val timeoutMs: Long? = 120_000L // 2 minute timeout
    ) : InputRequest()
    
    /**
     * Request for confirmation (e.g. confirm amount)
     */
    data class ConfirmationInput(
        override val id: String = UUID.randomUUID().toString(),
        val paymentId: String,
        val message: String,
        override val timeoutMs: Long? = 30_000L // 30 second timeout
    ) : InputRequest()
    
    /**
     * Request for selection from options
     */
    data class SelectionInput(
        override val id: String = UUID.randomUUID().toString(),
        val paymentId: String,
        val prompt: String,
        val options: List<String>,
        override val timeoutMs: Long? = 30_000L // 30 second timeout
    ) : InputRequest()
}
