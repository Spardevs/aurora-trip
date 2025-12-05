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

    /**
     * Request to confirm NFC keys
     */
    data class CONFIRM_NFC_KEYS(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 5_000L, // 5 seconds default timeout
    ) : UserInputRequest()

    /**
     * Request to confirm NFC tag authentication with a PIN and subject ID
     * @param pin The PIN to be used for NFC tag authentication
     * @param subjectId The subject ID from the tag to validate against
     */
    data class CONFIRM_NFC_TAG_AUTH(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 30_000L, // 30 seconds default timeout
        val pin: String,
        val subjectId: String
    ) : UserInputRequest()

    /**
     * Request to confirm NFC tag customer data
     */
    data class CONFIRM_NFC_TAG_CUSTOMER_DATA(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 300_000L, // 300 seconds default timeout
    ) : UserInputRequest()

    /**
     * Request to customer confirm they've saved the NFC tag PIN
     */
    data class CONFIRM_NFC_TAG_CUSTOMER_SAVE_PIN(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 90_000L, // 90 seconds default timeout
        val pin: String
    ) : UserInputRequest()

    /**
     * Request for printer paper cut confirmation
     */
    data class CONFIRM_PRINTER_PAPER_CUT(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 10_000L, // 10 seconds default timeout
    ) : UserInputRequest()
}