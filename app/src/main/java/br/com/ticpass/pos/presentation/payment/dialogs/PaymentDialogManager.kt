package br.com.ticpass.pos.presentation.payment.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.payment.viewmodel.PaymentProcessingViewModel
import br.com.ticpass.pos.presentation.payment.states.PaymentProcessingUiState
import br.com.ticpass.pos.core.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.common.view.TimeoutCountdownView
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.utils.toMoneyAsDouble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages all payment-related dialogs for the PaymentProcessingActivity.
 * This class extracts dialog logic to improve maintainability and testability.
 */
class PaymentDialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val paymentViewModel: PaymentProcessingViewModel
) {

    /**
     * Show a dialog to confirm proceeding to the next payment processor.
     * Allows editing payment method and amount before proceeding.
     */
    fun showConfirmNextPaymentProcessorDialog(requestId: String) {
        // Get the current UI state to access payment details
        val state = paymentViewModel.uiState.value as PaymentProcessingUiState.ConfirmNextProcessor<PaymentProcessingQueueItem>
        val currentPayment = state.currentItem
        
        Log.d("TimeoutDebug", "showConfirmNextPaymentProcessorDialog - state.timeoutMs: ${state.timeoutMs}")
        
        // Create a custom dialog view with editable fields
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_confirmation, null)
        
        // Get references to the editable fields
        val paymentInfoTextView = dialogView.findViewById<TextView>(R.id.text_payment_info)
        val amountEditText = dialogView.findViewById<EditText>(R.id.edit_payment_amount)
        val methodSpinner = dialogView.findViewById<Spinner>(R.id.spinner_payment_method)
        val timeoutView = dialogView.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)
        
        // Set initial values
        amountEditText.setText((currentPayment.amount.toMoneyAsDouble()).toString())

        paymentInfoTextView.text = if(paymentViewModel.fullSize == 1) {
            context.getString(R.string.next_payment_progress_first)
        } else {
            context.getString(
                R.string.next_payment_progress,
                paymentViewModel.currentIndex,
                paymentViewModel.fullSize,
            )
        }
        
        // Setup payment method spinner
        val paymentMethods = SystemPaymentMethod.values()
        val methodAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, paymentMethods)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        methodSpinner.adapter = methodAdapter
        methodSpinner.setSelection(paymentMethods.indexOf(currentPayment.method))
        
        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_next_processor_title)
            .setView(dialogView)
            .setPositiveButton(R.string.proceed, null) // Set to null initially to prevent auto-dismiss
            .setNegativeButton(R.string.abort, null) // Set to null initially to prevent auto-dismiss
            .setNeutralButton(R.string.skip, null) // Cancel button to cancel the current payment
            .setCancelable(false)
            .create()
            
        // Set button click listeners manually to prevent auto-dismiss
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                try {
                    // Get the modified values
                    val modifiedAmount = (amountEditText.text.toString().toDouble() * 1000).toInt()
                    val modifiedMethod = paymentMethods[methodSpinner.selectedItemPosition]

                    paymentViewModel.confirmProcessor(
                        requestId = requestId,
                        modifiedItem = currentPayment.copy(
                            amount = modifiedAmount,
                            method = modifiedMethod,
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
                paymentViewModel.abortPayment()
                dialog.dismiss()
            }
            
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener {
                // Skip the current payment
                paymentViewModel.skipProcessor(requestId)
                dialog.dismiss()
                // Show toast notification that payment was canceled
                Toast.makeText(context, R.string.skip, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, state.timeoutMs) {
            // Auto-skip on timeout
            paymentViewModel.skipProcessor(requestId)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * Show a dialog with error retry options.
     */
    fun showErrorRetryOptionsDialog(requestId: String, error: ProcessingErrorEvent) {
        // Get the current UI state to access timeout
        val state = paymentViewModel.uiState.value as? PaymentProcessingUiState.ErrorRetryOrSkip ?: return
        
        Log.d("TimeoutDebug", "showErrorRetryOptionsDialog - state.timeoutMs: ${state.timeoutMs}")
        
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        val errorMessage = context.getString(resourceId)
        
        // Also update the progress view with the error message
        Log.e("ErrorHandling", "showErrorRetryOptionsDialog updating progress view with error: $error")
        // Display error: $error
        
        // Create a custom dialog with multiple buttons and timeout
        val view = layoutInflater.inflate(R.layout.dialog_error_retry_options, null)
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
            paymentViewModel.retryPayment(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_skip).setOnClickListener {
            paymentViewModel.skipProcessorOnError(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_abort).setOnClickListener {
            paymentViewModel.abortPayment()
            dialog.dismiss()
        }
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, state.timeoutMs) {
            // Auto-skip on timeout
            paymentViewModel.skipProcessorOnError(requestId)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * Show a dialog to confirm customer receipt printing.
     */
    fun showCustomerReceiptDialog(requestId: String, timeoutMs: Long) {
        var isDialogAnswered = false
        var timeoutJob: Job? = null
        
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.print_customer_receipt_title)
            .setMessage(R.string.print_customer_receipt_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                isDialogAnswered = true
                timeoutJob?.cancel()
                paymentViewModel.confirmCustomerReceiptPrinting(requestId, true)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                isDialogAnswered = true
                timeoutJob?.cancel()
                paymentViewModel.confirmCustomerReceiptPrinting(requestId, false)
            }
            .setCancelable(false)
            .create()
        
        // Set up timeout mechanism if timeout is specified
        if (timeoutMs > 0) {
            timeoutJob = CoroutineScope(Dispatchers.IO).launch {
                delay(timeoutMs)
                if (!isDialogAnswered) {
                    isDialogAnswered = true
                    dialog.dismiss()
                    // Default answer on timeout: NO (don't print receipt)
                    paymentViewModel.confirmCustomerReceiptPrinting(requestId, true)
                }
            }
        }
        
        // Handle dialog dismissal to cancel timeout
        dialog.setOnDismissListener {
            if (!isDialogAnswered) {
                isDialogAnswered = true
                timeoutJob?.cancel()
            }
        }
        
        dialog.show()
    }

    /**
     * Show a dialog with PIX QR code for scanning.
     */
    fun showPixScanningDialog(requestId: String, pixCode: String) {
        // Create a dialog with custom view for QR code
        val dialogView = layoutInflater.inflate(R.layout.dialog_pix_qrcode, null)
        
        // Get the ImageView for QR code
        val qrCodeImageView = dialogView.findViewById<ImageView>(R.id.image_qr_code)
        
        // Generate QR code bitmap
        try {
            val qrCodeBitmap = generateQRCode(pixCode)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)
            
            // Create and show the dialog
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            // Set up dialog dismiss listener to recycle bitmap when dialog is dismissed
            dialog.setOnDismissListener {
                // Get the drawable from the ImageView and recycle the bitmap
                val drawable = qrCodeImageView.drawable
                if (drawable is BitmapDrawable) {
                    qrCodeBitmap.recycle()
                    drawable.bitmap?.recycle()
                }
            }
            
            // Set up Cancel button
            dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                paymentViewModel.confirmMerchantPixHasBeenPaid(requestId, false)
            }
            
            // Set up Done button
            dialogView.findViewById<Button>(R.id.btn_done).setOnClickListener {
                dialog.dismiss()
                paymentViewModel.confirmMerchantPixHasBeenPaid(requestId, true)
            }
            
            dialog.show()
        } catch (e: Exception) {
            Log.e("PaymentDialogManager", "Error generating QR code: ${e.message}")
            Toast.makeText(context, "Error generating QR code", Toast.LENGTH_SHORT).show()
            paymentViewModel.confirmMerchantPixHasBeenPaid(requestId, false)
        }
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

    /**
     * Generate a QR code bitmap from the given content.
     */
    private fun generateQRCode(content: String): Bitmap {
        val width = 500
        val height = 500
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.MARGIN] = 2
        
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
}
