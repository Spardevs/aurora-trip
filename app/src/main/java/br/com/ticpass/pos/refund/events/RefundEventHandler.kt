package br.com.ticpass.pos.refund.events

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.feature.refund.state.RefundUiEvent
import br.com.ticpass.pos.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.queue.processors.refund.models.RefundEventResourceMapper
import com.google.android.material.snackbar.Snackbar

/**
 * Handles all refund-related events for the RefundActivity.
 * This class extracts event handling logic to improve maintainability and testability.
 */
class RefundEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView,
) {

    /**
     * Handle refund processor events.
     * @param event The refund processor event to handle
     */
    fun handleRefundEvent(event: RefundEvent) {
        when (event) {
            is RefundEvent.START,
               RefundEvent.CANCELLED -> { /* no op */ }
            else -> { /* Other events don't affect PIN display */ }
        }
        
        val eventMessage = getEventMessage(event)

        Log.d("RefundEventHandler", "$eventMessage")
        
        // Update dialog with the event message
        dialogEventTextView.text = eventMessage
    }

    /**
     * Handle UI events from the ViewModel.
     * @param event The UI event to handle
     */
    fun handleUiEvent(event: RefundUiEvent) {
        when (event) {
            // Handle message events
            is RefundUiEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is RefundUiEvent.ShowSnackbar -> {
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
            is RefundUiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is RefundUiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        Log.d("RefundEventHandler", "Confirmation dialog: Yes")
                    }
                    .setNegativeButton("No") { _, _ ->
                        Log.d("RefundEventHandler", "Confirmation dialog: No")
                    }
                    .show()
                Log.d("RefundEventHandler", "Confirmation dialog: ${event.title} - ${event.message}")
            }
            
            // Handle refund events
            is RefundUiEvent.RefundCompleted -> {}
            is RefundUiEvent.RefundFailed -> {}
        }
    }

    /**
     * Get localized event message for a refund processor event.
     * @param event The refund processor event
     * @return Localized event message string
     */
    private fun getEventMessage(event: RefundEvent): String {
        val resourceKey = RefundEventResourceMapper.getErrorResourceKey(event)
        val message = context.getString(resourceKey)

        return message
    }
}
