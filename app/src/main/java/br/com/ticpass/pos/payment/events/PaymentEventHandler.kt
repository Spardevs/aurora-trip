package br.com.ticpass.pos.payment.events

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.TimeoutCountdownView
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEventResourceMapper
import com.google.android.material.snackbar.Snackbar

/**
 * Handles all payment-related events for the PaymentProcessingActivity.
 * This class extracts event handling logic to improve maintainability and testability.
 */
class PaymentEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView,
    private val dialogQRCodeImageView: ImageView,
    private val dialogTimeoutCountdownView: TimeoutCountdownView,
) {

    // For tracking PIN entry
    private val pinDigits = mutableListOf<Int>()

    /**
     * Handle payment processor events.
     * @param event The payment processor event to handle
     */
    fun handlePaymentEvent(event: ProcessingPaymentEvent) {
        when (event) {
            is ProcessingPaymentEvent.CARD_INSERTED,
               ProcessingPaymentEvent.CARD_REMOVAL_SUCCEEDED -> { clearPinDigits() }

            is ProcessingPaymentEvent.PIN_DIGIT_INPUT -> {
                pinDigits.add(1) // Add a digit placeholder
            }
            is ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> {
                if (pinDigits.isNotEmpty()) {
                    pinDigits.removeAt(pinDigits.lastIndex) // Remove last digit
                } else {
                    // sometimes acquirers will emit a PIN_DIGIT_REMOVED event
                    // even if no digits are present
                    clearPinDigits()
                }
            }
            is ProcessingPaymentEvent.PIN_REQUESTED -> {
                // Reset PIN digits when a new PIN is requested
                clearPinDigits()
            }
            is ProcessingPaymentEvent.PIN_OK -> {
                // Clear PIN digits when PIN is confirmed
                clearPinDigits()
            }
            is ProcessingPaymentEvent.QRCODE_SCAN -> {
                // Show QR code in the unified dialog
                showQRCodeInDialog(event.qrCode, event.timeoutMs)
                return // Don't update dialog text for QR code events
            }
            // Events that indicate QR code scanning is complete - hide QR code
            is ProcessingPaymentEvent.TRANSACTION_DONE,
            ProcessingPaymentEvent.APPROVAL_SUCCEEDED,
            ProcessingPaymentEvent.APPROVAL_DECLINED,
            ProcessingPaymentEvent.CANCELLED,
            ProcessingPaymentEvent.GENERIC_SUCCESS,
            ProcessingPaymentEvent.GENERIC_ERROR -> {
                // Hide QR code and timeout if showing
                hideQRCodeFromDialog()
            }
            else -> { /* Other events don't affect PIN display */ }
        }
        
        val eventMessage = getEventMessage(event)
        
        // Update dialog with the event message
        dialogEventTextView.text = eventMessage
    }

    /**
     * Handle UI events from the ViewModel.
     * @param event The UI event to handle
     */
    fun handleUiEvent(event: PaymentProcessingUiEvent) {
        when (event) {
            // Handle message events
            is PaymentProcessingUiEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is PaymentProcessingUiEvent.ShowSnackbar -> {
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
            is PaymentProcessingUiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is PaymentProcessingUiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        Log.d("PaymentEventHandler", "Confirmation dialog: Yes")
                    }
                    .setNegativeButton("No") { _, _ ->
                        Log.d("PaymentEventHandler", "Confirmation dialog: No")
                    }
                    .show()
                Log.d("PaymentEventHandler", "Confirmation dialog: ${event.title} - ${event.message}")
            }
            
            // Handle payment events
            is PaymentProcessingUiEvent.PaymentCompleted -> {
                val amountStr = event.amount.toString()
            }
            is PaymentProcessingUiEvent.PaymentFailed -> {}
        }
    }

    /**
     * Clear PIN digits (useful for external PIN management).
     */
    fun clearPinDigits() {
        pinDigits.clear()
    }

    /**
     * Show QR code in the unified dialog.
     */
    private fun showQRCodeInDialog(qrCode: android.graphics.Bitmap, timeoutMs: Long) {
        // Set the QR code bitmap
        dialogQRCodeImageView.setImageBitmap(qrCode)
        dialogQRCodeImageView.visibility = View.VISIBLE
        
        // Update the event text for QR code scanning
        dialogEventTextView.text = context.getString(R.string.qrcode_scan_message)
        
        // Start timeout countdown if specified
        if (timeoutMs > 0) {
            dialogTimeoutCountdownView.visibility = View.VISIBLE
            dialogTimeoutCountdownView.startCountdown(timeoutMs) {
                // Handle timeout - hide QR code
                hideQRCodeFromDialog()
            }
        }
    }
    
    /**
     * Hide QR code from the unified dialog.
     */
    private fun hideQRCodeFromDialog() {
        // Hide QR code image
        dialogQRCodeImageView.visibility = View.GONE
        
        // Clean up bitmap to prevent memory leaks
        val drawable = dialogQRCodeImageView.drawable
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.recycle()
        }
        dialogQRCodeImageView.setImageDrawable(null)
        
        // Hide and cancel timeout countdown
        dialogTimeoutCountdownView.visibility = View.GONE
        dialogTimeoutCountdownView.cancelCountdown()
    }

    /**
     * Get localized event message for a payment processor event.
     * @param event The payment processor event
     * @return Localized event message string
     */
    private fun getEventMessage(event: ProcessingPaymentEvent): String {
        val resourceKey = ProcessingPaymentEventResourceMapper.getErrorResourceKey(event)
        var message = context.getString(resourceKey)

        when (event) {
            is ProcessingPaymentEvent.PIN_DIGIT_INPUT,
               ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> {
                val pinDisplay = PaymentUIUtils.formatPinDisplay(pinDigits)
                message += " $pinDisplay"
            }
            else -> {}
        }

        return message
    }
}
