package br.com.ticpass.pos.feature.nfc.state

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Represents the UI state of the nfc processing screen
 */
sealed class NFCUiState {
    /**
     * Idle state - no processing happening
     */
    object Idle : NFCUiState()
    
    /**
     * Processing state - nfcs are being processed
     */
    object Processing : NFCUiState()
    
    /**
     * Error state - an error occurred during processing
     */
    data class Error(val event: ProcessingErrorEvent) : NFCUiState()
    
    /**
     * Confirmation state - waiting for user to confirm proceeding to next processor
     */
    data class ConfirmNextProcessor<T>(
        val requestId: String,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItem: T,  // Generic item data for UI access
        val timeoutMs: Long? = null
    ) : NFCUiState()
    
    /**
     * Error retry state - waiting for user to decide how to handle an error
     */
    data class ErrorRetryOrSkip(
        val requestId: String,
        val error: ProcessingErrorEvent,
        val timeoutMs: Long? = null
    ) : NFCUiState()

    /**
     * Confirm app NFC Tag Keys
     */
    data class ConfirmNFCKeys(
        val requestId: String,
        val timeoutMs: Long = 5_000L, // 5 seconds default timeout
    ) : NFCUiState()

    /**
     * Confirm NFC Tag Authentication
     * @param requestId Unique identifier for the request
     * @param timeoutMs Timeout in milliseconds for the confirmation
     * @param pin The PIN to be used for NFC tag authentication
     * @param subjectId The subject ID from the tag to validate against
     */
    data class ConfirmNFCTagAuth(
        val requestId: String,
        val timeoutMs: Long = 5_000L, // 5 seconds default timeout
        val pin: String,
        val subjectId: String
    ) : NFCUiState()

    /**
     * Confirm NFC Customer Data
     * @param requestId Unique identifier for the request
     * @param timeoutMs Timeout in milliseconds for the confirmation
     */
    data class ConfirmNFCCustomerData(
        val requestId: String,
        val timeoutMs: Long = 90_000L, // 90 seconds default timeout
    ) : NFCUiState()

    /**
     * Confirm NFC Customer Save PIN
     * @param requestId Unique identifier for the request
     * @param timeoutMs Timeout in milliseconds for the confirmation
     */
    data class ConfirmNFCCustomerSavePin(
        val requestId: String,
        val timeoutMs: Long = 90_000L, // 90 seconds default timeout
        val pin: String,
    ) : NFCUiState()
}
