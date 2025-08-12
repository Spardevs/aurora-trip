package br.com.ticpass.pos.queue.input

import java.util.UUID

/**
 * Input Request
 * Represents a request for user input during processing
 */
sealed class UserInputRequest {
    abstract val id: String
    abstract val timeoutMs: Long

    /**
     * Request for customer receipt printing confirmation
     */
    data class CONFIRM_CUSTOMER_RECEIPT_PRINTING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 10_000L, // 10 seconds default timeout
    ) : UserInputRequest()

    /**
     * Request for merchant receipt printing confirmation
     */
    data class CONFIRM_MERCHANT_PIX_KEY(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 60_000L, // 60 seconds default timeout
    ) : UserInputRequest()

    /**
     * Request for user to scan a merchant PIX QRCode
     * @param pixCode The PIX code to be scanned
     */
    data class MERCHANT_PIX_SCANNING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 60_000L, // 60 seconds default timeout
        val pixCode: String,
    ) : UserInputRequest()

    /**
     * Request to confirm printer network information like IP address, port, etc.
     */
    data class CONFIRM_PRINTER_NETWORK_INFO(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 5_000L, // 5 seconds default timeout
    ) : UserInputRequest()
}