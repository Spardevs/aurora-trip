package br.com.ticpass.pos.presentation.refund.states

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Represents the UI state of the refund processing screen
 */
sealed class RefundUiState {
    /**
     * Idle state - no processing happening
     */
    object Idle : RefundUiState()
    
    /**
     * Processing state - refunds are being processed
     */
    object Processing : RefundUiState()
    
    /**
     * Error state - an error occurred during processing
     */
    data class Error(val event: ProcessingErrorEvent) : RefundUiState()
    
    /**
     * Confirmation state - waiting for user to confirm proceeding to next processor
     */
    data class ConfirmNextProcessor<T>(
        val requestId: String,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItem: T,  // Generic item data for UI access
        val timeoutMs: Long? = null
    ) : RefundUiState()

    /**
     * Confirmation state - waiting for user to confirm printer network info
     */
    data class ConfirmPrinterNetworkInfo(
        val requestId: String,
        val timeoutMs: Long,
    ) : RefundUiState()
    
    /**
     * Error retry state - waiting for user to decide how to handle an error
     */
    data class ErrorRetryOrSkip(
        val requestId: String,
        val error: ProcessingErrorEvent,
        val timeoutMs: Long? = null
    ) : RefundUiState()
}
