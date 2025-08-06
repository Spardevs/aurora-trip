package br.com.ticpass.pos.feature.payment.state

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Represents a one-time UI event that should be consumed by the UI
 * These events are not part of the persistent state and are delivered only once
 */
sealed class PaymentProcessingUiEvent {
    // Message events
    data class ShowToast(val message: String) : PaymentProcessingUiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : PaymentProcessingUiEvent()
    
    // Dialog events
    data class ShowErrorDialog(val title: String, val message: String) : PaymentProcessingUiEvent()
    data class ShowConfirmationDialog(val title: String, val message: String) : PaymentProcessingUiEvent()
    
    // Payment events
    data class PaymentCompleted(val paymentId: String, val amount: Int) : PaymentProcessingUiEvent()
    data class PaymentFailed(val paymentId: String, val error: ProcessingErrorEvent) : PaymentProcessingUiEvent()
}
