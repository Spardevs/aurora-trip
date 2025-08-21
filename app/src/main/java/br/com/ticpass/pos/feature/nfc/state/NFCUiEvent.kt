package br.com.ticpass.pos.feature.nfc.state

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Represents a one-time UI event that should be consumed by the UI
 * These events are not part of the persistent state and are delivered only once
 */
sealed class NFCUiEvent {
    // Message events
    data class ShowToast(val message: String) : NFCUiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : NFCUiEvent()
    
    // Dialog events
    data class ShowErrorDialog(val title: String, val message: String) : NFCUiEvent()
    data class ShowConfirmationDialog(val title: String, val message: String) : NFCUiEvent()
    
    // NFC events
    data class NFCCompleted(val nfcId: String) : NFCUiEvent()
    data class NFCFailed(val nfcId: String, val error: ProcessingErrorEvent) : NFCUiEvent()
}
