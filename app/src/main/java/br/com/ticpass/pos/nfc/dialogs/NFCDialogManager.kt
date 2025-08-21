package br.com.ticpass.pos.nfc.dialogs

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.nfc.models.NFCTagCustomerDataInput
import br.com.ticpass.pos.util.BrazilianPhoneUtils
import br.com.ticpass.pos.util.CpfUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.UUID
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.nfc.NFCViewModel
import br.com.ticpass.pos.feature.nfc.state.NFCUiState
import br.com.ticpass.pos.nfc.models.SystemNFCMethod
import br.com.ticpass.pos.nfc.view.TimeoutCountdownView
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem

/**
 * Manages all nfc-related dialogs for the NFCActivity.
 * This class extracts dialog logic to improve maintainability and testability.
 */
class NFCDialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val nfcViewModel: NFCViewModel
) {

    /**
     * Show a dialog to confirm proceeding to the next nfc processor.
     * Allows editing nfc method and amount before proceeding.
     */
    fun showConfirmNextNFCProcessorDialog(requestId: String) {
        // Get the current UI state to access nfc details
        val state = nfcViewModel.uiState.value as NFCUiState.ConfirmNextProcessor<NFCQueueItem>
        val currentNFC = state.currentItem
        
        Log.d("TimeoutDebug", "showConfirmNextNFCProcessorDialog - state.timeoutMs: ${state.timeoutMs}")
        
        // Create a custom dialog view with editable fields
        val dialogView = layoutInflater.inflate(R.layout.dialog_nfc_confirmation, null)
        
        // Get references to the editable fields
        val nfcInfoTextView = dialogView.findViewById<TextView>(R.id.text_nfc_info)
        val methodSpinner = dialogView.findViewById<Spinner>(R.id.spinner_nfc_method)
        val timeoutView = dialogView.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)

        nfcInfoTextView.text = if(nfcViewModel.fullSize == 1) {
            context.getString(R.string.next_nfc_progress_first)
        } else {
            context.getString(
                R.string.next_nfc_progress,
                nfcViewModel.currentIndex,
                nfcViewModel.fullSize,
            )
        }
        
        // Setup nfc method spinner
        val nfcMethods = SystemNFCMethod.entries.map { it.toString() };
        val methodAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, nfcMethods)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        methodSpinner.adapter = methodAdapter
        methodSpinner.setSelection(nfcMethods.indexOf(currentNFC.processorType.toString()))
        
        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_next_processor_title)
            .setView(dialogView)
            .setPositiveButton(R.string.proceed, null) // Set to null initially to prevent auto-dismiss
            .setNegativeButton(R.string.abort, null) // Set to null initially to prevent auto-dismiss
            .setNeutralButton(R.string.skip, null) // Cancel button to cancel the current nfc
            .setCancelable(false)
            .create()
            
        // Set button click listeners manually to prevent auto-dismiss
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                try {

                    val modifiedItem = when(currentNFC) {
                        is NFCQueueItem.TagFormatOperation -> {
                            currentNFC.copy(
                                bruteForce = currentNFC.bruteForce,
                            )
                        }
                        else -> { currentNFC }
                    }

                    nfcViewModel.confirmProcessor(
                        requestId = requestId,
                        modifiedItem = modifiedItem
                    )

                    dialog.dismiss()
                } catch (e: Exception) {
                    // Handle parsing errors
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show()
                }
            }
            
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                nfcViewModel.abortNFC()
                dialog.dismiss()
            }
            
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener {
                // Skip the current nfc
                nfcViewModel.skipProcessor(requestId)
                dialog.dismiss()
                // Show toast notification that nfc was canceled
                Toast.makeText(context, R.string.skip, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, state.timeoutMs) {
            // Auto-skip on timeout
            nfcViewModel.skipProcessor(requestId)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    fun showConfirmNFCCustomerSavePin(
        requestId: String,
        timeoutMs: Long,
        pin: String,
    ) {
        // Create a custom dialog view for PIN memorization
        val dialogView = layoutInflater.inflate(R.layout.dialog_nfc_pin_memorization, null)
        
        // Get references to the UI elements
        val textPinDisplay = dialogView.findViewById<TextView>(R.id.text_pin_display)
        val timeoutView = dialogView.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)
        val btnMemorized = dialogView.findViewById<Button>(R.id.btn_memorized)
        
        // Display the PIN prominently
        textPinDisplay.text = pin
        
        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // Set up button listener with try-catch to always return true
        btnMemorized.setOnClickListener {
            try {
                nfcViewModel.confirmNFCCustomerSavePin(requestId, true)
                dialog.dismiss()
            } catch (e: Exception) {
                // Always ensure we return true even if there's an error
                Log.e("NFCDialogManager", "Error in confirmNFCCustomerSavePin, but continuing: ${e.message}")
                try {
                    nfcViewModel.confirmNFCCustomerSavePin(requestId, true)
                } catch (e2: Exception) {
                    Log.e("NFCDialogManager", "Second attempt failed, dismissing dialog: ${e2.message}")
                }
                dialog.dismiss()
            }
        }
        
        // Show dialog
        dialog.show()
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, timeoutMs) {
            // Auto-confirm on timeout (always return true)
            try {
                nfcViewModel.confirmNFCCustomerSavePin(requestId, true)
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e("NFCDialogManager", "Error on timeout, but continuing: ${e.message}")
                dialog.dismiss()
            }
        }
    }

    fun showConfirmNFCCustomerData(
        requestId: String,
        timeoutMs: Long,
    ) {
        // Create a custom dialog view for customer data entry
        val dialogView = layoutInflater.inflate(R.layout.dialog_nfc_customer_data, null)
        
        // Get references to the UI elements
        val layoutName = dialogView.findViewById<TextInputLayout>(R.id.layout_name)
        val editName = dialogView.findViewById<TextInputEditText>(R.id.edit_name)
        val layoutCpf = dialogView.findViewById<TextInputLayout>(R.id.layout_cpf)
        val editCpf = dialogView.findViewById<TextInputEditText>(R.id.edit_cpf)
        val layoutPhone = dialogView.findViewById<TextInputLayout>(R.id.layout_phone)
        val editPhone = dialogView.findViewById<TextInputEditText>(R.id.edit_phone)
        val timeoutView = dialogView.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        
        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // Set up text watchers for formatting and validation
        editCpf.addTextChangedListener(CpfUtils.createCpfTextWatcher())
        editPhone.addTextChangedListener(BrazilianPhoneUtils.createBrazilianPhoneTextWatcher())
        
        // Function to validate all fields
        fun validateFields(): Boolean {
            var isValid = true
            
            // Validate name length
            val name = editName.text.toString().trim()
            if (name.length > 30) {
                layoutName.error = context.getString(R.string.nfc_customer_error_name_too_long)
                isValid = false
            } else {
                layoutName.error = null
            }
            
            // Validate CPF if provided
            val cpf = editCpf.text.toString().trim()
            if (cpf.isNotEmpty() && !CpfUtils.isValidCpf(cpf)) {
                layoutCpf.error = context.getString(R.string.nfc_customer_error_invalid_cpf)
                isValid = false
            } else {
                layoutCpf.error = null
            }
            
            // Validate phone if provided
            val phone = editPhone.text.toString().trim()
            if (phone.isNotEmpty() && !BrazilianPhoneUtils.isValidBrazilianPhone(phone)) {
                layoutPhone.error = context.getString(R.string.nfc_customer_error_invalid_phone)
                isValid = false
            } else {
                layoutPhone.error = null
            }
            
            return isValid
        }
        
        // Function to create customer data from form
        fun createCustomerData(): NFCTagCustomerDataInput {
            return NFCTagCustomerDataInput(
                id = UUID.randomUUID().toString(),
                name = editName.text.toString().trim(),
                nationalId = CpfUtils.cleanCpf(editCpf.text.toString()),
                phone = BrazilianPhoneUtils.cleanPhone(editPhone.text.toString())
            )
        }
        
        // Set up button listeners
        btnCancel.setOnClickListener {
            nfcViewModel.confirmNFCCustomerData(requestId, null)
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            if (validateFields()) {
                val customerData = createCustomerData()
                nfcViewModel.confirmNFCCustomerData(requestId, customerData)
                dialog.dismiss()
            }
        }
        
        // Show dialog and focus on name field
        dialog.show()
        editName.requestFocus()
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, timeoutMs) {
            // Auto-cancel on timeout
            nfcViewModel.confirmNFCCustomerData(requestId, null)
            dialog.dismiss()
        }
    }

    /**
     * Show a dialog to confirm NFC tag authentication with a PIN.
     * Allows user to enter the PIN for NFC tag authentication.
     */
    fun showConfirmNFCTagAuthDialog(
        requestId: String,
        timeoutMs: Long,
        pin: String,
    ) {
        // manager pin should be retrieved from secure storage in production app
        val managerPin = "0000"
        var attemptsRemaining = 3
        
        // Create a custom dialog view for PIN entry
        val dialogView = layoutInflater.inflate(R.layout.dialog_nfc_pin_auth, null)
        
        // Get references to the UI elements
        val editPin = dialogView.findViewById<EditText>(R.id.edit_pin)
        val textAttempts = dialogView.findViewById<TextView>(R.id.text_attempts)
        val timeoutView = dialogView.findViewById<TimeoutCountdownView>(R.id.timeout_countdown_view)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        
        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // Function to hide keyboard
        fun hideKeyboard() {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editPin.windowToken, 0)
        }
        
        // Function to validate PIN
        fun validatePin(): Boolean {
            val enteredPin = editPin.text.toString()
            
            if (enteredPin.length != 4) {
                Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (enteredPin == pin || enteredPin == managerPin) {
                hideKeyboard()
                nfcViewModel.confirmNFCTagAuth(requestId, true)
                dialog.dismiss()
                return true
            } else {
                attemptsRemaining--
                
                if (attemptsRemaining > 0) {
                    // Show attempts remaining
                    textAttempts.text = context.getString(R.string.nfc_pin_attempts_remaining, attemptsRemaining)
                    textAttempts.visibility = View.VISIBLE
                    
                    // Clear the PIN field and show error
                    editPin.text.clear()
                    Toast.makeText(context, R.string.nfc_pin_error_wrong, Toast.LENGTH_SHORT).show()
                    
                    return false
                } else {
                    // Max attempts reached
                    hideKeyboard()
                    Toast.makeText(context, R.string.nfc_pin_error_max_attempts, Toast.LENGTH_LONG).show()
                    nfcViewModel.confirmNFCTagAuth(requestId, false)
                    dialog.dismiss()
                    return true
                }
            }
        }
        
        // Set up text watcher to enable/disable confirm button
        editPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnConfirm.isEnabled = s?.length == 4
            }
        })
        
        // Set up button listeners
        btnCancel.setOnClickListener {
            hideKeyboard()
            nfcViewModel.confirmNFCTagAuth(requestId, false)
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            validatePin()
        }
        
        // Initially disable confirm button
        btnConfirm.isEnabled = false
        
        // Show dialog and focus on PIN field
        dialog.show()
        editPin.requestFocus()
        
        // Show keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editPin, InputMethodManager.SHOW_IMPLICIT)
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, timeoutMs) {
            // Auto-fail authentication on timeout
            hideKeyboard()
            nfcViewModel.confirmNFCTagAuth(requestId, false)
            dialog.dismiss()
        }
    }
    

    /**
     * Show a dialog with error retry options.
     */
    fun showErrorRetryOptionsDialog(requestId: String, error: ProcessingErrorEvent) {
        // Get the current UI state to access timeout
        val state = nfcViewModel.uiState.value as? NFCUiState.ErrorRetryOrSkip ?: return
        
        Log.d("TimeoutDebug", "showErrorRetryOptionsDialog - state.timeoutMs: ${state.timeoutMs}")
        
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        val errorMessage = context.getString(resourceId)
        
        // Also update the progress view with the error message
        Log.e("ErrorHandling", "showErrorRetryOptionsDialog updating progress view with error: $error")
        // Display error: $error
        
        // Create a custom dialog with multiple buttons and timeout
        val view = layoutInflater.inflate(R.layout.nfc_dialog_error_retry_options, null)
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
            nfcViewModel.retryNFC(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_skip).setOnClickListener {
            nfcViewModel.skipProcessorOnError(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_abort).setOnClickListener {
            nfcViewModel.abortNFC()
            dialog.dismiss()
        }
        
        // Start timeout countdown if specified
        startDialogTimeoutCountdown(timeoutView, state.timeoutMs) {
            // Auto-skip on timeout
            nfcViewModel.skipProcessorOnError(requestId)
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
