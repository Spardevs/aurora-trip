package br.com.ticpass.pos.payment

import android.app.AlertDialog
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.payment.InteractivePaymentViewModel
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.payment.SystemPaymentMethod
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class PaymentProcessingActivity : AppCompatActivity() {
    
    // ViewModel injected via Hilt
    private val viewModel: InteractivePaymentViewModel by viewModels()
    
    // UI components
    private lateinit var queueSizeTextView: TextView
    private lateinit var processingProgressTextView: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var currentEventTextView: TextView
    private lateinit var eventLogTextView: TextView
    private lateinit var queueRecyclerView: RecyclerView
    private lateinit var queueAdapter: PaymentQueueAdapter
    
    // Progress Dialog
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: android.widget.ProgressBar
    private lateinit var dialogEventTextView: TextView
    
    // PIN entry tracking
    private val pinDigits = mutableListOf<Int>()
    
    // Tracking current processing
    private var totalPayments = 0
    private var currentProcessingIndex = 0
    private val eventLog = StringBuilder()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_processing)
        
        setupViews()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupViews() {
        queueSizeTextView = findViewById(R.id.text_queue_size)
        processingProgressTextView = findViewById(R.id.text_processing_progress)
        progressBar = findViewById(R.id.progress_bar)
        currentEventTextView = findViewById(R.id.text_current_event)
        eventLogTextView = findViewById(R.id.text_event_log)
        eventLogTextView.movementMethod = ScrollingMovementMethod()
        
        // Setup RecyclerView
        queueRecyclerView = findViewById(R.id.recycler_queue_items)
        queueRecyclerView.layoutManager = LinearLayoutManager(this)
        queueAdapter = PaymentQueueAdapter(emptyList()) { paymentId ->
            viewModel.cancelPayment(paymentId)
        }
        queueRecyclerView.adapter = queueAdapter
        
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
            viewModel.cancelAllPayments()
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
        
        // Start processing button
        findViewById<View>(R.id.btn_start_processing).setOnClickListener {
            viewModel.startProcessing()
            // The confirmation dialog will be shown automatically via UI state observation
            // and the progress dialog will be shown when processing actually starts
            Log.d("PaymentProcessingActivity", "Waiting for confirmation dialog")
        }
        
        // Cancel all payments button
        findViewById<View>(R.id.btn_cancel_all).setOnClickListener {
            viewModel.cancelAllPayments()
        }
    }
    
    private fun observeViewModel() {
        // Observe queue state
        lifecycleScope.launch {
            viewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems)
            }
        }
        
        // Observe UI events (one-time events from the ViewModel)
        lifecycleScope.launch {
            viewModel.uiEvents.collect { event ->
                handleUiEvent(event)
            }
        }
        
        // Observe processing state
        lifecycleScope.launch {
            viewModel.processingState.collectLatest { state ->
                Log.d("PaymentProcessingActivity", "Processing state: $state")
                
                // Show dialog for active processing states
                if (state is ProcessingState.ItemProcessing || 
                    state is ProcessingState.ItemRetrying) {
                    showProgressDialog()
                }
                
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        val currentIndex = viewModel.queueState.value.indexOfFirst { it.id == state.item.id }
                        val total = viewModel.queueState.value.size
                        updateProcessingProgress(currentIndex, total)
                    }
                    is ProcessingState.ItemDone -> {
                        addEventLogMessage("All payments completed")
                    }
                    is ProcessingState.ItemFailed -> {
                        // Handle error state
                        Log.e("PaymentProcessingActivity", "Processing failed: ${state.error}")
                        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(state.error)
                        val displayMessage = getString(resourceId)
                        Log.e("PaymentProcessingActivity", "Error message: $displayMessage")
                        addEventLogMessage("Error: $displayMessage")
                        
                        // Also display the error in the progress area
                        displayErrorMessage(state.error)
                    }
                    is ProcessingState.ItemRetrying -> {
                        // Update progress for retrying state
                        val currentIndex = viewModel.queueState.value.indexOfFirst { it.id == state.item.id }
                        updateProcessingProgress(currentIndex, totalPayments)
                    }
                    is ProcessingState.ItemSkipped -> {
                        // Update progress for skipped state
                        val currentIndex = viewModel.queueState.value.indexOfFirst { it.id == state.item.id }
                        updateProcessingProgress(currentIndex, totalPayments)
                    }
                    is ProcessingState.QueueCanceled -> {
                        // Reset progress when queue is canceled
                        updateProcessingProgress(0, 0)
                        addEventLogMessage("All payments canceled")
                    }
                    is ProcessingState.QueueDone -> {
                        // Reset progress when queue processing is done
                        updateProcessingProgress(0, 0)
                        addEventLogMessage("All payments processed successfully")
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
            viewModel.processingPaymentEvents.collectLatest { event ->
                handlePaymentEvent(event)
            }
        }
        
        // Observe UI state for input dialogs and error states
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is InteractivePaymentViewModel.UiState.ConfirmNextPaymentProcessor -> {
                        showConfirmNextPaymentProcessorDialog(
                            requestId = uiState.requestId,
                            currentIndex = uiState.currentItemIndex,
                            totalItems = uiState.totalItems
                        )
                    }
                    is InteractivePaymentViewModel.UiState.Error -> {
                        displayErrorMessage(uiState.event)
                    }
                    is InteractivePaymentViewModel.UiState.ConfirmNextProcessor -> {
                        showConfirmNextProcessorDialog(
                            requestId = uiState.requestId,
                            currentIndex = uiState.currentItemIndex,
                            totalItems = uiState.totalItems
                        )
                    }
                    is InteractivePaymentViewModel.UiState.ConfirmCustomerReceiptPrinting -> {
                        showCustomerReceiptDialog(uiState.requestId)
                    }
                    is InteractivePaymentViewModel.UiState.ErrorRetryOrSkip -> {
                        showErrorRetryOptionsDialog(uiState.requestId, uiState.error)
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
        queueSizeTextView.text = "Items in queue: ${queueItems.size}"
        queueAdapter.updateItems(queueItems)
        totalPayments = queueItems.size
        progressBar.max = totalPayments
    }
    
    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current
        processingProgressTextView.text = getString(R.string.payment_progress, current, total)
        // Reset text color to default when showing normal progress
        processingProgressTextView.setTextColor(getColor(android.R.color.black))
        progressBar.progress = current
        
        // Update dialog progress
        dialogProgressTextView.text = getString(R.string.payment_progress, current, total)
        dialogProgressBar.progress = current
        dialogProgressBar.max = total
        
        // Show dialog if processing is happening, hide otherwise
        if (total > 0) { // Changed condition to show dialog whenever there are items in the queue
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
        val pinDisplay = "*".repeat(pinDigits.size)
        val pinMessage = if (pinDigits.isEmpty()) {
            getString(R.string.event_pin_requested)
        } else {
            getString(R.string.pin_entry_display, pinDisplay)
        }
        
        // Update the dialog with the PIN display
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
        
        // Update both the main UI and dialog with the event message
        currentEventTextView.text = eventMessage
        dialogEventTextView.text = eventMessage
        addEventLogMessage(eventMessage)
    }
    
    /**
     * Handle one-time UI events from the ViewModel
     */
    private fun handleUiEvent(event: InteractivePaymentViewModel.UiEvent) {
        when (event) {
            // Handle navigation events
            is InteractivePaymentViewModel.UiEvent.NavigateBack -> {
                finish()
            }
            is InteractivePaymentViewModel.UiEvent.NavigateToPaymentDetails -> {
                // Example: Navigate to payment details
                // val intent = Intent(this, PaymentDetailsActivity::class.java)
                // intent.putExtra("paymentId", event.paymentId)
                // startActivity(intent)
                addEventLogMessage("Navigate to payment details: ${event.paymentId}")
            }
            
            // Handle message events
            is InteractivePaymentViewModel.UiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                addEventLogMessage("Toast: ${event.message}")
            }
            is InteractivePaymentViewModel.UiEvent.ShowSnackbar -> {
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
                addEventLogMessage("Snackbar: ${event.message}")
            }
            
            // Handle dialog events
            is InteractivePaymentViewModel.UiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
                addEventLogMessage("Error dialog: ${event.title} - ${event.message}")
            }
            is InteractivePaymentViewModel.UiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        addEventLogMessage("Confirmation dialog: Yes")
                    }
                    .setNegativeButton("No") { _, _ ->
                        addEventLogMessage("Confirmation dialog: No")
                    }
                    .show()
                addEventLogMessage("Confirmation dialog: ${event.title} - ${event.message}")
            }
            
            // Handle payment events
            is InteractivePaymentViewModel.UiEvent.PaymentCompleted -> {
                val amountStr = event.amount.toString()
                addEventLogMessage("Payment ${event.paymentId} completed: $amountStr")
            }
            is InteractivePaymentViewModel.UiEvent.PaymentFailed -> {
                addEventLogMessage("Payment ${event.paymentId} failed: ${event.error}")
            }
        }
    }
    
    private fun addEventLogMessage(message: String) {
        eventLog.append("• $message\n")
        eventLogTextView.text = eventLog.toString()
        // Scroll to the bottom
        val scrollAmount = eventLogTextView.layout?.getLineTop(eventLogTextView.lineCount) ?: 0
        eventLogTextView.scrollTo(0, scrollAmount)
    }
    
    private fun showCustomerReceiptDialog(requestId: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.print_customer_receipt_title)
            .setMessage(R.string.print_customer_receipt_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.confirmCustomerReceiptPrinting(requestId, true)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                viewModel.confirmCustomerReceiptPrinting(requestId, false)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun enqueuePayment(method: SystemPaymentMethod) {
        // Generate a random amount between R$10 and R$200
        val amount = (1000..20000).random()
        val commission = 0 // No commission for example
        
        viewModel.enqueuePayment(
            amount = amount,
            commission = commission,
            method = method,
            processorType = "acquirer"
        )
    }
    
    /**
     * Display error message in the progress area
     * This overrides the current progress display with an error message
     */
    private fun displayErrorMessage(error: ProcessingErrorEvent) {
        // Get the string resource key from the error event using the mapper
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        val errorMessage = getString(resourceId)
        Log.d("PaymentProcessingActivity", "Displaying error: $errorMessage")

        // Display error in progress area with error styling
        processingProgressTextView.text = errorMessage
        processingProgressTextView.setTextColor(getColor(android.R.color.holo_red_dark))

        // Also log the error in the event log
        addEventLogMessage(getString(R.string.event_fail, errorMessage))

        // Update current event text as well
        currentEventTextView.text = errorMessage
        
        // Update dialog with error message
        dialogEventTextView.text = errorMessage
        
        // Make sure dialog is showing for errors
        showProgressDialog()
        currentEventTextView.setTextColor(getColor(android.R.color.holo_red_dark))
    }
    
    /**
     * Show a dialog to confirm proceeding to the next processor (generic version)
     */
    private fun showConfirmNextProcessorDialog(requestId: String, currentIndex: Int, totalItems: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_next_processor_title)
            .setMessage(getString(R.string.confirm_next_processor_message, currentIndex + 1, totalItems))
            .setPositiveButton(R.string.proceed) { _, _ ->
                viewModel.confirmNextProcessor(requestId)
            }
            .setNegativeButton(R.string.skip) { _, _ ->
                viewModel.skipProcessor(requestId)
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show a dialog to confirm proceeding to the next payment processor
     * Allows editing payment method and amount before proceeding
     */
    private fun showConfirmNextPaymentProcessorDialog(requestId: String, currentIndex: Int, totalItems: Int) {
        // Get the current UI state to access payment details
        val state = viewModel.uiState.value as? InteractivePaymentViewModel.UiState.ConfirmNextPaymentProcessor ?: return
        
        // Create a custom dialog view with editable fields
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_confirmation, null)
        
        // Get references to the editable fields
        val amountEditText = dialogView.findViewById<EditText>(R.id.edit_payment_amount)
        val methodSpinner = dialogView.findViewById<Spinner>(R.id.spinner_payment_method)
        val processorTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinner_processor_type)
        
        // Set initial values
        amountEditText.setText((state.currentAmount / 100.0).toString())
        
        // Setup payment method spinner
        val paymentMethods = SystemPaymentMethod.values()
        val methodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethods)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        methodSpinner.adapter = methodAdapter
        methodSpinner.setSelection(paymentMethods.indexOf(state.currentMethod))
        
        // Setup processor type spinner
        val processorTypes = arrayOf("acquirer", "cash", "transactionless")
        val processorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, processorTypes)
        processorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        processorTypeSpinner.adapter = processorAdapter
        processorTypeSpinner.setSelection(processorTypes.indexOf(state.currentProcessorType))
        
        // Create and show the dialog
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_next_processor_title)
            .setView(dialogView)
            .setPositiveButton(R.string.proceed) { _, _ ->
                try {
                    // Get the modified values
                    val modifiedAmount = (amountEditText.text.toString().toDouble() * 100).toInt()
                    val modifiedMethod = paymentMethods[methodSpinner.selectedItemPosition]
                    val modifiedProcessorType = processorTypes[processorTypeSpinner.selectedItemPosition]
                    
                    // Check if any values were modified
                    if (modifiedAmount != state.currentAmount || 
                        modifiedMethod != state.currentMethod || 
                        modifiedProcessorType != state.currentProcessorType) {
                        // Use the modified payment details
                        viewModel.confirmNextProcessorWithModifiedPayment(
                            requestId = requestId,
                            modifiedAmount = modifiedAmount,
                            modifiedMethod = modifiedMethod,
                            modifiedProcessorType = modifiedProcessorType
                        )
                    } else {
                        // Use the original payment details
                        viewModel.confirmNextProcessor(requestId)
                    }
                } catch (e: Exception) {
                    // Handle parsing errors
                    Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show()
                    // Use the original payment details
                    viewModel.confirmNextProcessor(requestId)
                }
            }
            .setNegativeButton(R.string.skip) { _, _ ->
                viewModel.skipProcessor(requestId)
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show a dialog with error retry options
     */
    private fun showErrorRetryOptionsDialog(requestId: String, error: ProcessingErrorEvent) {
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        val errorMessage = getString(resourceId)
        
        // Create a custom dialog with multiple buttons
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.error_retry_title)
            .setMessage(errorMessage)
            .setCancelable(false)
            .create()
            
        // Use a custom layout with multiple buttons
        val view = layoutInflater.inflate(R.layout.dialog_error_retry_options, null)
        dialog.setView(view)
        
        // Set up button click listeners
        view.findViewById<View>(R.id.btn_retry_immediately).setOnClickListener {
            viewModel.retryFailedPaymentImmediately(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_retry_later).setOnClickListener {
            viewModel.retryFailedPaymentLater(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_abort_current).setOnClickListener {
            viewModel.abortCurrentProcessor(requestId)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btn_abort_all).setOnClickListener {
            viewModel.abortAllProcessors(requestId)
            dialog.dismiss()
        }
        
        dialog.show()
    }
}

/**
 * Adapter for the payment queue RecyclerView
 */
class PaymentQueueAdapter(
    private var items: List<ProcessingPaymentQueueItem>,
    private val onCancelClicked: (String) -> Unit
) : RecyclerView.Adapter<PaymentQueueAdapter.ViewHolder>() {
    
    private fun formatCurrency(amount: BigDecimal): String {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        return format.format(amount)
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val methodTextView: TextView = view.findViewById(R.id.text_payment_method)
        val amountTextView: TextView = view.findViewById(R.id.text_payment_amount)
        val cancelButton: ImageButton = view.findViewById(R.id.btn_cancel_payment)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_queue, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.methodTextView.text = item.method.toString()
        
        val amountString = formatCurrency(BigDecimal(item.amount / 100.0))
        holder.amountTextView.text = amountString
        
        holder.cancelButton.setOnClickListener {
            onCancelClicked(item.id)
        }
    }
    
    override fun getItemCount() = items.size
    
    fun updateItems(newItems: List<ProcessingPaymentQueueItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}