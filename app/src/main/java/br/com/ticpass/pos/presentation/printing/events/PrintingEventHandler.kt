package br.com.ticpass.pos.presentation.printing.events

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.presentation.printing.states.PrintingUiEvent
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEventResourceMapper
import com.google.android.material.snackbar.Snackbar

/**
 * Handles all printing-related events for the PrintingActivity.
 * This class extracts event handling logic to improve maintainability and testability.
 */
class PrintingEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView,
) {

    /**
     * Handle printing processor events.
     * @param event The printing processor event to handle
     */
    fun handlePrintingEvent(event: PrintingEvent) {
        when (event) {
            is PrintingEvent.START,
               PrintingEvent.CANCELLED -> { /* no op */ }
            else -> { /* Other events don't affect PIN display */ }
        }
        
        val eventMessage = getEventMessage(event)

        Log.d("PrintingEventHandler", "$eventMessage")
        
        // Update dialog with the event message
        dialogEventTextView.text = eventMessage
    }

    /**
     * Handle UI events from the ViewModel.
     * @param event The UI event to handle
     */
    fun handleUiEvent(event: PrintingUiEvent) {
        when (event) {
            // Handle message events
            is PrintingUiEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is PrintingUiEvent.ShowSnackbar -> {
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
            is PrintingUiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is PrintingUiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        Log.d("PrintingEventHandler", "Confirmation dialog: Yes")
                    }
                    .setNegativeButton("No") { _, _ ->
                        Log.d("PrintingEventHandler", "Confirmation dialog: No")
                    }
                    .show()
                Log.d("PrintingEventHandler", "Confirmation dialog: ${event.title} - ${event.message}")
            }
            
            // Handle printing events
            is PrintingUiEvent.PrintingCompleted -> {}
            is PrintingUiEvent.PrintingFailed -> {}
        }
    }

    /**
     * Get localized event message for a printing processor event.
     * @param event The printing processor event
     * @return Localized event message string
     */
    private fun getEventMessage(event: PrintingEvent): String {
        val resourceKey = PrintingEventResourceMapper.getErrorResourceKey(event)
        val message = context.getString(resourceKey)

        return message
    }
}
