package br.com.ticpass.pos.queue.payment.state

import br.com.ticpass.pos.queue.ProcessingErrorEvent

/**
 * Represents a one-time UI event that should be consumed by the UI
 * These events are not part of the persistent state and are delivered only once
 */
sealed class UiEvent {
    // Navigation events
    object NavigateBack : UiEvent()
    data class NavigateToPaymentDetails(val paymentId: String) : UiEvent()
    
    // Message events
    data class ShowToast(val message: String) : UiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : UiEvent()
    
    // Dialog events
    data class ShowErrorDialog(val title: String, val message: String) : UiEvent()
    data class ShowConfirmationDialog(val title: String, val message: String) : UiEvent()
    
    // Payment events
    data class PaymentCompleted(val paymentId: String, val amount: Int) : UiEvent()
    data class PaymentFailed(val paymentId: String, val error: ProcessingErrorEvent) : UiEvent()
}
