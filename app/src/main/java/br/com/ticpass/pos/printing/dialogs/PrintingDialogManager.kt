package br.com.ticpass.pos.printing.dialogs

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.feature.printing.state.PrintingUiState
import br.com.ticpass.pos.printing.models.SystemPrintingMethod
import br.com.ticpass.pos.printing.view.TimeoutCountdownView
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType

/**
 * Manages all printing-related dialogs for the PrintingActivity.
 * This class extracts dialog logic to improve maintainability and testability.
 */
class PrintingDialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val printingViewModel: PrintingViewModel
) {

    /**
     * Show a dialog to confirm proceeding to the next printing processor.
     * Allows editing printing method and amount before proceeding.
     */
    fun showConfirmNextPrintingProcessorDialog(requestId: String) {
        // Get the current UI state to access printing details
        val state = printingViewModel.uiState.value as PrintingUiState.ConfirmNextProcessor<PrintingQueueItem>
        val currentPrinting = state.currentItem
        
        Log.d("TimeoutDebug", "showConfirmNextPrintingProcessorDialog - state.timeoutMs: ${state.timeoutMs}")
        
        // Create a custom dialog view with editable fields
        val dialogView = layoutInflater.inflate(R.layout.dialog_printing_confirmation, null)
        
        // Get references to the editable fields
        val printingInfoTextView = dialogView.findViewById<TextView>(R.id.text_printing_info)
        val methodSpinner = dialogView.findViewById<Spinner>(R.id.spinner_printing_method)
        val timeoutView = dialogView.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)

        printingInfoTextView.text = if(printingViewModel.fullSize == 1) {
            context.getString(R.string.next_printing_progress_first)
        } else {
            context.getString(
                R.string.next_printing_progress,
                printingViewModel.currentIndex,
                printingViewModel.fullSize,
            )
        }
        
        // Setup printing method spinner
        val printingMethods = SystemPrintingMethod.entries.map { it.toString() };
        val methodAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, printingMethods)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        methodSpinner.adapter = methodAdapter
        methodSpinner.setSelection(printingMethods.indexOf(currentPrinting.processorType.toString()))
        
        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_next_processor_title)
            .setView(dialogView)
            .setPositiveButton(R.string.proceed, null) // Set to null initially to prevent auto-dismiss
            .setNegativeButton(R.string.abort, null) // Set to null initially to prevent auto-dismiss
            .setNeutralButton(R.string.skip, null) // Cancel button to cancel the current printing
            .setCancelable(false)
            .create()
            
        // Set button click listeners manually to prevent auto-dismiss
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                try {
                    // Get the modified values
                    val modifiedMethod = printingMethods[methodSpinner.selectedItemPosition]
                    val processorType = PrintingProcessorType.entries.find { it.toString() == modifiedMethod }
                        ?: throw IllegalArgumentException("Unknown printing method: $modifiedMethod")

                    printingViewModel.confirmProcessor(
                        requestId = requestId,
                        modifiedItem = currentPrinting.copy(
                            processorType = processorType,
                        )
                    )

                    dialog.dismiss()
                } catch (e: Exception) {
                    // Handle parsing errors
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show()
                }
            }
            
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                printingViewModel.abortPrinting()
                dialog.dismiss()
            }
            
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener {
                // Skip the current printing
                printingViewModel.skipProcessor(requestId)
                dialog.dismiss()
                // Show toast notification that printing was canceled
                Toast.makeText(context, R.string.skip, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, state.timeoutMs) {
            // Auto-skip on timeout
            printingViewModel.skipProcessor(requestId)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * Show a dialog with error retry options.
     */
    fun showErrorRetryOptionsDialog(requestId: String, error: ProcessingErrorEvent) {
        // Get the current UI state to access timeout
        val state = printingViewModel.uiState.value as? PrintingUiState.ErrorRetryOrSkip ?: return
        
        Log.d("TimeoutDebug", "showErrorRetryOptionsDialog - state.timeoutMs: ${state.timeoutMs}")
        
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        val errorMessage = context.getString(resourceId)
        
        // Also update the progress view with the error message
        Log.e("ErrorHandling", "showErrorRetryOptionsDialog updating progress view with error: $error")
        // Display error: $error
        
        // Create a custom dialog with multiple buttons and timeout
        val view = layoutInflater.inflate(R.layout.printing_dialog_error_retry_options, null)
        val timeoutView = view.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)
        
        // Set error description with the specific error message
        view.findViewById<TextView>(R.id.text_error_description).text = errorMessage
        
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.error_retry_title)
            .setCancelable(false)
            .setView(view)
            .create()
        
        // Set up button click listeners
        view.findViewById<View>(R.id.btn_retry).setOnClickListener {
            printingViewModel.retryPrinting(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_skip).setOnClickListener {
            printingViewModel.skipProcessorOnError(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_abort).setOnClickListener {
            printingViewModel.abortPrinting()
            dialog.dismiss()
        }
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, state.timeoutMs) {
            // Auto-skip on timeout
            printingViewModel.skipProcessorOnError(requestId)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * Start the timeout countdown in a dialog if a timeout is specified.
     * 
     * @param timeoutView The TimeoutCountdownView in the dialog
     * @param timeoutMs The timeout duration in milliseconds
     * @param onTimeout Callback to be invoked when the timeout occurs
     */
    private fun startDialogTimeoutCountdown(timeoutView: TimeoutCountdownView?, timeoutMs: Long?, onTimeout: () -> Unit) {
        // Start the countdown if a timeout is specified and view exists
        if (timeoutView != null && timeoutMs != null && timeoutMs > 0) {
            timeoutView.visibility = View.VISIBLE
            timeoutView.startCountdown(timeoutMs, onTimeout)
        } else if (timeoutView != null) {
            // Hide the countdown view if no timeout is specified
            timeoutView.visibility = View.GONE
        } else {
            Log.e("TimeoutDebug", "Cannot start countdown - timeoutView is null")
        }
    }
}
