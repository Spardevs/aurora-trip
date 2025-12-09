package br.com.ticpass.pos.presentation.printing.states

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Represents the UI state of the printing processing screen
 */
sealed class PrintingUiState {
    /**
     * Idle state - no processing happening
     */
    object Idle : PrintingUiState()
    
    /**
     * Processing state - printings are being processed
     */
    object Processing : PrintingUiState()
    
    /**
     * Error state - an error occurred during processing
     */
    data class Error(val event: ProcessingErrorEvent) : PrintingUiState()
    
    /**
     * Confirmation state - waiting for user to confirm proceeding to next processor
     */
    data class ConfirmNextProcessor<T>(
        val requestId: String,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItem: T,  // Generic item data for UI access
        val timeoutMs: Long? = null
    ) : PrintingUiState()

    /**
     * Confirmation state - waiting for user to confirm printer network info
     */
    data class ConfirmPrinterNetworkInfo(
        val requestId: String,
        val timeoutMs: Long,
    ) : PrintingUiState()
    
    /**
     * Confirmation state - waiting for user to confirm printer paper cut type
     */
    data class ConfirmPrinterPaperCut(
        val requestId: String,
        val timeoutMs: Long,
    ) : PrintingUiState()
    
    /**
     * Error retry state - waiting for user to decide how to handle an error
     */
    data class ErrorRetryOrSkip(
        val requestId: String,
        val error: ProcessingErrorEvent,
        val timeoutMs: Long? = null
    ) : PrintingUiState()
}
