package br.com.ticpass.pos.payment

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R


import br.com.ticpass.pos.payment.view.PaymentProcessingQueueView
import br.com.ticpass.pos.payment.view.TimeoutCountdownView
import br.com.ticpass.pos.payment.dialogs.PaymentDialogManager
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType
import br.com.ticpass.pos.queue.processors.payment.processors.utils.PaymentMethodProcessorMapper
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.utils.toMoneyAsDouble
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentProcessingActivity : AppCompatActivity() {
    
    // ViewModel injected via Hilt
    private val paymentViewModel: PaymentProcessingViewModel by viewModels()
    
    // Dialog manager for handling all payment dialogs
    private lateinit var dialogManager: PaymentDialogManager
    
    // UI components
    private lateinit var queueView: PaymentProcessingQueueView
    
    // Progress Dialog
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: android.widget.ProgressBar
    private lateinit var dialogEventTextView: TextView
    
    // For tracking PIN entry
    private val pinDigits = mutableListOf<Int>()
    
    // For tracking progress
    private var totalPayments = 0
    private var currentProcessingIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_processing)
        
        setupViews()
        setupButtons()
        observeViewModel()
        
        // Initialize dialog manager
        dialogManager = PaymentDialogManager(this, layoutInflater, paymentViewModel)
    }
    
    private fun setupViews() {
        // Initialize custom views
        queueView = findViewById(R.id.payment_queue_view)
        
        // Set up payment queue view cancel callback
        queueView.onPaymentCanceled = { paymentId ->
            paymentViewModel.cancelPayment(paymentId)
        }
        
        // Set up transactionless checkbox listener
        findViewById<android.widget.CheckBox>(R.id.checkbox_transactionless).setOnCheckedChangeListener { _, isChecked ->
            // Update all queued items when checkbox state changes
            paymentViewModel.updateAllProcessorTypes(isChecked)
        }
        
        // Create progress dialog
        createProgressDialog()
    }
    
    private fun createProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_progress, null)
        
        // Get dialog views
        dialogProgressTextView = dialogView.findViewById(R.id.text_dialog_progress)
        dialogProgressBar = dialogView.findViewById(R.id.progress_bar_dialog)
        dialogEventTextView = dialogView.findViewById(R.id.text_dialog_event)
        
        // Setup cancel button
        dialogView.findViewById<View>(R.id.btn_dialog_cancel).setOnClickListener {
            paymentViewModel.cancelAllPayments()
        }
        
        // Create dialog
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    
    private fun setupButtons() {
        // Enqueue different payment types
        findViewById<View>(R.id.btn_add_credit).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.CREDIT)
        }
        
        findViewById<View>(R.id.btn_add_debit).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.DEBIT)
        }
        
        findViewById<View>(R.id.btn_add_voucher).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.VOUCHER)
        }
        
        findViewById<View>(R.id.btn_add_pix).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.PIX)
        }
        
        findViewById<View>(R.id.btn_add_personal_pix).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.MERCHANT_PIX)
        }
        
        findViewById<View>(R.id.btn_add_cash).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.CASH)
        }
        
        findViewById<View>(R.id.btn_add_bitcoin_ln).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.LN_BITCOIN)
        }
        
        // Start processing button
        findViewById<View>(R.id.btn_start_processing).setOnClickListener {
            paymentViewModel.startProcessing()
            // The confirmation dialog will be shown automatically via UI state observation
            // and the progress dialog will be shown when processing actually starts
            Log.d("PaymentProcessingActivity", "Waiting for confirmation dialog")
        }
        
        // Cancel all payments button
        findViewById<View>(R.id.btn_cancel_all).setOnClickListener {
            paymentViewModel.cancelAllPayments()
        }
    }
    
    private fun observeViewModel() {
        // Observe queue state
        lifecycleScope.launch {
            paymentViewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems)
            }
        }
        
        // Observe UI events (one-time events from the ViewModel)
        lifecycleScope.launch {
            paymentViewModel.uiEvents.collect { event ->
                handleUiEvent(event)
            }
        }
        
        // Observe processing state
        lifecycleScope.launch {
            paymentViewModel.processingState.collectLatest { state ->
                Log.d("PaymentProcessingActivity", "Processing state: $state")
                
                // Show dialog for active processing states
                if (state is ProcessingState.ItemProcessing || 
                    state is ProcessingState.ItemRetrying) {
                    showProgressDialog()
                }
                
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        val currentIndex = paymentViewModel.queueState.value.indexOfFirst { it.id == state.item.id }
                        val total = paymentViewModel.queueState.value.size
                        updateProcessingProgress(currentIndex, total)
                    }
                    is ProcessingState.ItemDone -> {
                        // Success: All payments completed
                    }
                    is ProcessingState.ItemFailed -> {
                        // Handle error state
                        Log.e("PaymentProcessingActivity", "Processing failed: ${state.error}")
                        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(state.error)
                        val displayMessage = getString(resourceId)
                        Log.e("PaymentProcessingActivity", "Error message: $displayMessage")
                        // Error: $displayMessage
                        
                        // Also display the error in the progress area
                        displayErrorMessage(state.error)
                    }
                    is ProcessingState.ItemRetrying -> {
                        // Update progress for retrying state
                        val currentIndex = paymentViewModel.queueState.value.indexOfFirst { it.id == state.item.id }
                        updateProcessingProgress(currentIndex, totalPayments)
                    }
                    is ProcessingState.ItemSkipped -> {
                        // Update progress for skipped state
                        val currentIndex = paymentViewModel.queueState.value.indexOfFirst { it.id == state.item.id }
                        updateProcessingProgress(currentIndex, totalPayments)
                    }
                    is ProcessingState.QueueCanceled -> {
                        // Reset progress when queue is canceled
                        updateProcessingProgress(0, 0)
                        // All payments canceled
                    }
                    is ProcessingState.QueueDone -> {
                        // Reset progress when queue processing is done
                        updateProcessingProgress(0, 0)
                        // Success: All payments processed successfully
                    }
                    else -> { 
                        // Reset progress for other states
                        updateProcessingProgress(0, 0)
                    }
                }
            }
        }
        
        // Observe payment events
        lifecycleScope.launch {
            paymentViewModel.processingPaymentEvents.collectLatest { event ->
                handlePaymentEvent(event)
            }
        }
        
        // Observe UI state for input dialogs and error states
        lifecycleScope.launch {
            paymentViewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is PaymentProcessingUiState.Error -> {
                        displayErrorMessage(uiState.event)
                    }
                    is PaymentProcessingUiState.ConfirmNextProcessor<*> -> {
                        dialogManager.showConfirmNextPaymentProcessorDialog(uiState.requestId)
                    }
                    is PaymentProcessingUiState.ConfirmCustomerReceiptPrinting -> {
                        dialogManager.showCustomerReceiptDialog(uiState.requestId)
                    }
                    is PaymentProcessingUiState.MerchantPixScanning -> {
                        dialogManager.showPixScanningDialog(uiState.requestId, uiState.pixCode)
                    }
                    is PaymentProcessingUiState.ConfirmMerchantPixKey -> {
                        confirmMerchantPixKey(uiState.requestId)
                    }
                    is PaymentProcessingUiState.ErrorRetryOrSkip -> {
                        dialogManager.showErrorRetryOptionsDialog(uiState.requestId, uiState.error)
                    }
                    else -> {
                        // Other UI states don't need dialogs
                        Log.d("PaymentProcessingActivity", "No dialog needed for state: $uiState")
                    }
                }
            }
        }
    }
    
    private fun updateQueueUI(queueItems: List<ProcessingPaymentQueueItem>) {
        queueView.updateQueue(queueItems)
        totalPayments = queueItems.size
        
        // Update the queue title with item count
        findViewById<TextView>(R.id.text_payment_queue_title).text = 
            getString(R.string.payment_queue, queueItems.size)
    }
    
    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current
        
        // Update progress in the main view
        // Progress update: $current of $total
        
        // Update dialog progress
        dialogProgressTextView.text = getString(R.string.payment_progress, current, total)
        dialogProgressBar.progress = current
        dialogProgressBar.max = total
        
        // Show dialog if processing is happening, hide otherwise
        if (total > 0) { // Show dialog whenever there are items in the queue
            showProgressDialog()
        } else {
            hideProgressDialog()
        }
    }
    
    private fun showProgressDialog() {
        if (progressDialog?.isShowing != true) {
            progressDialog?.show()
        }
    }
    
    private fun hideProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
    }
    
    /**
     * Updates the PIN display in the UI with asterisks based on the current pinDigits list
     */
    private fun updatePinDisplay() {
        val pinMessage = PaymentUIUtils.generatePinDisplayMessage(pinDigits)
        
        // Update the progress dialog with the PIN display
        dialogProgressTextView.text = "Processing payment..."
        
        // Update the event text with PIN information
        dialogEventTextView.text = pinMessage
    }
    

    
    private fun handlePaymentEvent(event: ProcessingPaymentEvent) {
        // Handle PIN digit tracking
        when (event) {
            is ProcessingPaymentEvent.PIN_DIGIT_INPUT -> {
                pinDigits.add(1) // Add a digit placeholder
                updatePinDisplay()
            }
            is ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> {
                if (pinDigits.isNotEmpty()) {
                    pinDigits.removeAt(pinDigits.lastIndex) // Remove last digit
                    updatePinDisplay()
                } else {
                    // sometimes acquirers will emit a PIN_DIGIT_REMOVED event
                    // even if no digits are present
                    pinDigits.clear()
                }
            }
            is ProcessingPaymentEvent.PIN_REQUESTED -> {
                // Reset PIN digits when a new PIN is requested
                pinDigits.clear()
                updatePinDisplay()
            }
            is ProcessingPaymentEvent.PIN_OK -> {
                // Clear PIN digits when PIN is confirmed
                pinDigits.clear()
            }
            else -> { /* Other events don't affect PIN display */ }
        }
        
        val eventMessage = when (event) {
            is ProcessingPaymentEvent.START -> getString(R.string.event_start)
            is ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> getString(R.string.event_card_reach_or_insert)
            is ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> getString(R.string.event_approval_succeeded)
            is ProcessingPaymentEvent.APPROVAL_DECLINED -> getString(R.string.event_approval_declined)
            is ProcessingPaymentEvent.TRANSACTION_DONE -> getString(R.string.event_transaction_done)
            is ProcessingPaymentEvent.TRANSACTION_PROCESSING -> getString(R.string.event_transaction_processing)
            is ProcessingPaymentEvent.AUTHORIZING -> getString(R.string.event_authorizing)
            is ProcessingPaymentEvent.CARD_BIN_REQUESTED -> getString(R.string.event_card_bin_requested)
            is ProcessingPaymentEvent.CARD_BIN_OK -> getString(R.string.event_card_bin_ok)
            is ProcessingPaymentEvent.CARD_HOLDER_REQUESTED -> getString(R.string.event_card_holder_requested)
            is ProcessingPaymentEvent.CARD_HOLDER_OK -> getString(R.string.event_card_holder_ok)
            is ProcessingPaymentEvent.CONTACTLESS_ERROR -> getString(R.string.event_contactless_error)
            is ProcessingPaymentEvent.CONTACTLESS_ON_DEVICE -> getString(R.string.event_contactless_on_device)
            is ProcessingPaymentEvent.CVV_OK -> getString(R.string.event_cvv_ok)
            is ProcessingPaymentEvent.CVV_REQUESTED -> getString(R.string.event_cvv_requested)
            is ProcessingPaymentEvent.DOWNLOADING_TABLES -> getString(R.string.event_downloading_tables)
            is ProcessingPaymentEvent.SAVING_TABLES -> getString(R.string.event_saving_tables)
            is ProcessingPaymentEvent.USE_CHIP -> getString(R.string.event_use_chip)
            is ProcessingPaymentEvent.USE_MAGNETIC_STRIPE -> getString(R.string.event_use_magnetic_stripe)
            is ProcessingPaymentEvent.CARD_REMOVAL_REQUESTING -> getString(R.string.event_card_removal_requesting)
            is ProcessingPaymentEvent.KEY_INSERTED -> getString(R.string.event_key_inserted)
            is ProcessingPaymentEvent.ACTIVATION_SUCCEEDED -> getString(R.string.event_activation_succeeded)
            is ProcessingPaymentEvent.SOLVING_PENDING_ISSUES -> getString(R.string.event_solving_pending_issues)
            is ProcessingPaymentEvent.PIN_REQUESTED -> getString(R.string.event_pin_requested)
            is ProcessingPaymentEvent.CARD_INSERTED -> {
                pinDigits.clear()
                getString(R.string.event_card_inserted)
            }
            is ProcessingPaymentEvent.PIN_DIGIT_INPUT -> {
                // For PIN input, show the PIN with asterisks
                val pinDisplay = "*".repeat(pinDigits.size)
                getString(R.string.event_pin_digit_input) + " $pinDisplay"
            }
            is ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> {
                // For PIN removal, show the updated PIN with asterisks
                val pinDisplay = "*".repeat(pinDigits.size)
                getString(R.string.event_pin_digit_removed) + " $pinDisplay"
            }
            is ProcessingPaymentEvent.CARD_REMOVAL_SUCCEEDED -> {
                pinDigits.clear()
                getString(R.string.event_card_removal_succeeded)
            }
            is ProcessingPaymentEvent.PIN_OK -> getString(R.string.event_pin_ok)
            is ProcessingPaymentEvent.GENERIC_SUCCESS -> getString(R.string.event_generic_success)
            is ProcessingPaymentEvent.GENERIC_ERROR -> getString(R.string.event_generic_error)
            ProcessingPaymentEvent.CANCELLED -> getString(R.string.event_cancelled)
        }
        
        // Update the payment progress view with the event message
        // Event message update: $eventMessage
        
        // Update dialog with the event message
        dialogEventTextView.text = eventMessage
        
        // Log the payment event using the dedicated method
        // Payment event: $eventMessage
    }
    
    /**
     * Handle one-time UI events from the ViewModel
     */
    private fun handleUiEvent(event: PaymentProcessingUiEvent) {
        when (event) {
            // Handle navigation events
            is PaymentProcessingUiEvent.NavigateBack -> {
                finish()
            }
            is PaymentProcessingUiEvent.NavigateToPaymentDetails -> {
                // Example: Navigate to payment details
                // val intent = Intent(this, PaymentDetailsActivity::class.java)
                // intent.putExtra("paymentId", event.paymentId)
                // startActivity(intent)
                // Navigate to payment details: ${event.paymentId}
            }
            
            // Handle message events
            is PaymentProcessingUiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                // Toast message: ${event.message}
            }
            is PaymentProcessingUiEvent.ShowSnackbar -> {
                val view = findViewById<View>(android.R.id.content)
                val snackbar = com.google.android.material.snackbar.Snackbar.make(
                    view, 
                    event.message, 
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                )
                event.actionLabel?.let { label ->
                    snackbar.setAction(label) {
                        // Handle snackbar action if needed
                    }
                }
                snackbar.show()
                // Snackbar message: ${event.message}
            }
            
            // Handle dialog events
            is PaymentProcessingUiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
                // Error dialog: ${event.title} - ${event.message}
            }
            is PaymentProcessingUiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        // Confirmation dialog: Yes
                    }
                    .setNegativeButton("No") { _, _ ->
                        // Confirmation dialog: No
                    }
                    .show()
                // Confirmation dialog: ${event.title} - ${event.message}
            }
            
            // Handle payment events
            is PaymentProcessingUiEvent.PaymentCompleted -> {
                val amountStr = event.amount.toString()
                // Payment ${event.paymentId} completed: $amountStr
            }
            is PaymentProcessingUiEvent.PaymentFailed -> {
                // Payment ${event.paymentId} failed: ${event.error}
            }
        }
    }
    
    // We now use EventLogView's methods directly
    

    
    /**
     * Enqueue a payment with the specified method and processor type
     * Uses ACQUIRER as the default processor type, or TRANSACTIONLESS if the checkbox is checked
     */
    private fun enqueuePayment(method: SystemPaymentMethod) {
        val transactionlessCheckbox = findViewById<android.widget.CheckBox>(R.id.checkbox_transactionless)
        val isTransactionlessEnabled = PaymentUIUtils.isTransactionlessModeEnabled(transactionlessCheckbox)
        
        val paymentData = PaymentUIUtils.createPaymentData(
            method = method,
            isTransactionlessEnabled = isTransactionlessEnabled
        )
        
        paymentViewModel.enqueuePayment(
            amount = paymentData.amount,
            commission = paymentData.commission,
            method = paymentData.method,
            processorType = paymentData.processorType
        )
    }
    
    /**
     * Display error message in the progress area
     * This overrides the current progress display with an error message
     */
    private fun displayErrorMessage(error: ProcessingErrorEvent) {
        val errorMessage = PaymentUIUtils.getErrorMessage(this, error)
        PaymentUIUtils.logError("PaymentProcessingActivity", error, this)

        // Display error in progress area with error styling
        // Display error: $error

        // Also log the error in the event log using the dedicated error method
        // Error: $errorMessage
        
        // Update dialog with error message
        dialogEventTextView.text = errorMessage
        
        // Make sure dialog is showing for errors
        showProgressDialog()
    }
    
    /**
     * Handle PIX key input request
     * 
     * NOTE: In a real-world application, this value should be fetched from a database
     * or user preferences instead of being hardcoded. This is just for demonstration purposes.
     */
    private fun confirmMerchantPixKey(requestId: String) {
        val pixKey = PaymentUIUtils.getHardcodedPixKey()
        
        // Directly confirm the PIX key without showing a dialog
        paymentViewModel.confirmMerchantPixKey(requestId, pixKey)
    }


    

    

    

}

// PaymentQueueAdapter has been moved to PaymentProcessingQueueView custom view