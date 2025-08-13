package br.com.ticpass.pos.feature.refund.state

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Represents a one-time UI event that should be consumed by the UI
 * These events are not part of the persistent state and are delivered only once
 */
sealed class RefundUiEvent {
    // Message events
    data class ShowToast(val message: String) : RefundUiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : RefundUiEvent()
    
    // Dialog events
    data class ShowErrorDialog(val title: String, val message: String) : RefundUiEvent()
    data class ShowConfirmationDialog(val title: String, val message: String) : RefundUiEvent()
    
    // Refund events
    data class RefundCompleted(val refundId: String) : RefundUiEvent()
    data class RefundFailed(val refundId: String, val error: ProcessingErrorEvent) : RefundUiEvent()
}
