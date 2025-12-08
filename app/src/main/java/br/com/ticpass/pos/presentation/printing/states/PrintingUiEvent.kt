package br.com.ticpass.pos.presentation.printing.states

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Represents a one-time UI event that should be consumed by the UI
 * These events are not part of the persistent state and are delivered only once
 */
sealed class PrintingUiEvent {
    // Message events
    data class ShowToast(val message: String) : PrintingUiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : PrintingUiEvent()
    
    // Dialog events
    data class ShowErrorDialog(val title: String, val message: String) : PrintingUiEvent()
    data class ShowConfirmationDialog(val title: String, val message: String) : PrintingUiEvent()
    
    // Printing events
    data class PrintingCompleted(val printingId: String) : PrintingUiEvent()
    data class PrintingFailed(val printingId: String, val error: ProcessingErrorEvent) : PrintingUiEvent()
}
