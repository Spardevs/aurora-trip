package br.com.ticpass.pos.queue.payment.state

import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.payment.SystemPaymentMethod
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType

/**
 * Represents the UI state of the payment processing screen
 */
sealed class UiState {
    /**
     * Idle state - no processing happening
     */
    object Idle : UiState()
    
    /**
     * Processing state - payments are being processed
     */
    object Processing : UiState()
    
    /**
     * Error state - an error occurred during processing
     */
    data class Error(val event: ProcessingErrorEvent) : UiState()
    
    /**
     * Confirmation state - waiting for user to confirm proceeding to next processor
     */
    data class ConfirmNextProcessor(
        val requestId: String,
        val currentItemIndex: Int,
        val totalItems: Int
    ) : UiState()
    
    /**
     * Confirmation state - waiting for user to confirm proceeding to next payment processor
     * Includes payment details that can be modified
     */
    data class ConfirmNextPaymentProcessor(
        val requestId: String,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentAmount: Int,
        val currentMethod: SystemPaymentMethod,
        val currentProcessorType: PaymentProcessorType,
        val timeoutMs: Long? = null
    ) : UiState()
    
    /**
     * Confirmation state - waiting for user to confirm customer receipt printing
     */
    data class ConfirmCustomerReceiptPrinting(
        val requestId: String
    ) : UiState()

    /**
     * PIX scanning state - waiting for user to scan a PIX code
     */
    data class MerchantPixScanning(
        val requestId: String,
        val pixCode: String,
    ) : UiState()
    
    /**
     * Error retry state - waiting for user to decide how to handle an error
     */
    data class ErrorRetryOrSkip(
        val requestId: String,
        val error: ProcessingErrorEvent,
        val timeoutMs: Long? = null
    ) : UiState()
    
    /**
     * Confirmation state - waiting for user to input merchant PIX key
     */
    data class ConfirmMerchantPixKey(
        val requestId: String,
        val timeoutMs: Long? = null
    ) : UiState()
}
