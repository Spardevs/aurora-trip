package br.com.ticpass.pos.queue.input

import java.util.UUID

/**
 * Input Request
 * Represents a request for user input during processing
 */
sealed class InputRequest {
    abstract val id: String
    abstract val timeoutMs: Long?

    /**
     * Request for customer receipt printing confirmation
     */
    data class CONFIRM_CUSTOMER_RECEIPT_PRINTING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long? = 10_000L, // 10 seconds default timeout
    ) : InputRequest()

    data class CONFIRM_MERCHANT_PIX_KEY(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long? = 60_000L, // 60 seconds default timeout
    ) : InputRequest()

    data class MERCHANT_PIX_SCANNING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long? = 60_000L, // 60 seconds default timeout
        val pixCode: String,
    ) : InputRequest()
}
