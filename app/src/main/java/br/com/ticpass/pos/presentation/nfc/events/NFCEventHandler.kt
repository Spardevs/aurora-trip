package br.com.ticpass.pos.presentation.nfc.events

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.nfc.states.NFCUiEvent
import br.com.ticpass.pos.common.view.TimeoutCountdownView
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEventResourceMapper
import com.google.android.material.snackbar.Snackbar

/**
 * Handles all nfc-related events for the NFCActivity.
 * This class extracts event handling logic to improve maintainability and testability.
 */
class NFCEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView,
    private val dialogTimeoutCountdownView: TimeoutCountdownView,
) {

    /**
     * Handle nfc processor events.
     * @param event The nfc processor event to handle
     */
    fun handleNFCEvent(event: NFCEvent) {
        when (event) {
            is NFCEvent.REACH_TAG -> {
                showTagReachDialog(event.timeoutMs)
                return
            }
            else -> { /* no-op */ }
        }
        
        val eventMessage = getEventMessage(event)

        Log.d("NFCEventHandler", "$eventMessage")
        
        // Update dialog with the event message
        dialogEventTextView.text = eventMessage
        hideTagReachDialog()
    }

    /**
     * Handle UI events from the ViewModel.
     * @param event The UI event to handle
     */
    fun handleUiEvent(event: NFCUiEvent) {
        when (event) {
            // Handle message events
            is NFCUiEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is NFCUiEvent.ShowSnackbar -> {
                val view = (context as android.app.Activity).findViewById<View>(android.R.id.content)
                val snackbar = Snackbar.make(
                    view, 
                    event.message, 
                    Snackbar.LENGTH_LONG
                )
                event.actionLabel?.let { label ->
                    snackbar.setAction(label) {
                        // Handle snackbar action if needed
                    }
                }
                snackbar.show()
            }
            
            // Handle dialog events
            is NFCUiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is NFCUiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        Log.d("NFCEventHandler", "Confirmation dialog: Yes")
                    }
                    .setNegativeButton("No") { _, _ ->
                        Log.d("NFCEventHandler", "Confirmation dialog: No")
                    }
                    .show()
                Log.d("NFCEventHandler", "Confirmation dialog: ${event.title} - ${event.message}")
            }
            
            // Handle nfc events
            is NFCUiEvent.NFCCompleted -> {}
            is NFCUiEvent.NFCFailed -> {}
        }
    }

    /**
     * Get localized event message for a nfc processor event.
     * @param event The nfc processor event
     * @return Localized event message string
     */
    private fun getEventMessage(event: NFCEvent): String {
        val resourceKey = NFCEventResourceMapper.getErrorResourceKey(event)
        val message = context.getString(resourceKey)

        return message
    }

    /**
     * Hide NFC tag reach dialog.
     */
    private fun hideTagReachDialog() {
        dialogTimeoutCountdownView.visibility = View.GONE
        dialogTimeoutCountdownView.cancelCountdown()
    }

    /**
     * Show NFC tag reach dialog with timeout.
     */
    private fun showTagReachDialog(timeoutMs: Long) {
        // Update the event text for QR code scanning
        dialogEventTextView.text = context.getString(R.string.event_nfc_reach_tag)

        // Start timeout countdown if specified
        if (timeoutMs > 0) {
            dialogTimeoutCountdownView.visibility = View.VISIBLE
            dialogTimeoutCountdownView.startCountdown(timeoutMs) {
                hideTagReachDialog()
            }
        }
    }
}
