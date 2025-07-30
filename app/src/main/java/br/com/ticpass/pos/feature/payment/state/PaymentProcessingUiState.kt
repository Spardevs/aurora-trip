package br.com.ticpass.pos.feature.payment.state

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Represents the UI state of the payment processing screen
 */
sealed class PaymentProcessingUiState {
    /**
     * Idle state - no processing happening
     */
    object Idle : PaymentProcessingUiState()
    
    /**
     * Processing state - payments are being processed
     */
    object Processing : PaymentProcessingUiState()
    
    /**
     * Error state - an error occurred during processing
     */
    data class Error(val event: ProcessingErrorEvent) : PaymentProcessingUiState()
    
    /**
     * Confirmation state - waiting for user to confirm proceeding to next processor
     */
    data class ConfirmNextProcessor<T>(
        val requestId: String,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItem: T,  // Generic item data for UI access
        val timeoutMs: Long? = null
    ) : PaymentProcessingUiState()
    
    /**
     * Confirmation state - waiting for user to confirm customer receipt printing
     */
    data class ConfirmCustomerReceiptPrinting(
        val requestId: String,
        val timeoutMs: Long,
    ) : PaymentProcessingUiState()

    /**
     * PIX scanning state - waiting for user to scan a PIX code
     */
    data class MerchantPixScanning(
        val requestId: String,
        val pixCode: String,
    ) : PaymentProcessingUiState()
    
    /**
     * Error retry state - waiting for user to decide how to handle an error
     */
    data class ErrorRetryOrSkip(
        val requestId: String,
        val error: ProcessingErrorEvent,
        val timeoutMs: Long? = null
    ) : PaymentProcessingUiState()
    
    /**
     * Confirmation state - waiting for user to input merchant PIX key
     */
    data class ConfirmMerchantPixKey(
        val requestId: String,
        val timeoutMs: Long? = null
    ) : PaymentProcessingUiState()
}
