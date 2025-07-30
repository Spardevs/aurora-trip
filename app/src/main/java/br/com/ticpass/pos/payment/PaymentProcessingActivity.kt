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
import br.com.ticpass.pos.payment.events.PaymentEventHandler
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
    
    // Event handler for handling all payment events
    private lateinit var eventHandler: PaymentEventHandler
    
    // UI components
    private lateinit var queueView: PaymentProcessingQueueView
    
    // Progress Dialog
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: android.widget.ProgressBar
    private lateinit var dialogEventTextView: TextView
    
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
        
        // Initialize event handler
        eventHandler = PaymentEventHandler(
            context = this,
            dialogEventTextView = dialogEventTextView,
            onPinDisplayUpdate = { updatePinDisplay() }
        )
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
                eventHandler.handleUiEvent(event)
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
                eventHandler.handlePaymentEvent(event)
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
                        dialogManager.showCustomerReceiptDialog(uiState.requestId, uiState.timeoutMs)
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
        val pinDigits = eventHandler.getCurrentPinDigits()
        val pinMessage = PaymentUIUtils.generatePinDisplayMessage(pinDigits)
        
        // Update the event text with PIN information
        dialogEventTextView.text = pinMessage
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