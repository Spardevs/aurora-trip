package br.com.ticpass.pos.payment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.coordination.PaymentActivityCoordinator
import br.com.ticpass.pos.payment.dialogs.PaymentDialogManager
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.PaymentProcessingQueueView
import br.com.ticpass.pos.sdk.AcquirerSdk
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentProcessingActivity : AppCompatActivity() {
    
    // ViewModel injected via Hilt
    private val paymentViewModel: PaymentProcessingViewModel by viewModels()
    
    // Specialized components for handling different responsibilities
    private lateinit var dialogManager: PaymentDialogManager
    private lateinit var eventHandler: PaymentEventHandler
    private lateinit var coordinator: PaymentActivityCoordinator
    
    // UI components
    private lateinit var queueView: PaymentProcessingQueueView
    
    // Progress Dialog components
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: ProgressBar
    private lateinit var dialogEventTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize SDK
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_processing)
        
        // Basic view setup
        setupViews()
        setupButtons()
        
        // Initialize specialized components
        initializeComponents()
        
        // Start coordination
        coordinator.initialize()
    }
    
    private fun setupViews() {
        // Initialize queue view
        queueView = findViewById(R.id.payment_queue_view)
        
        // Set up payment queue view cancel callback
        queueView.onPaymentCanceled = { paymentId ->
            paymentViewModel.cancelPayment(paymentId)
        }
        
        // Set up transactionless checkbox listener
        val transactionlessCheckbox = findViewById<android.widget.CheckBox>(R.id.checkbox_transactionless)
        transactionlessCheckbox.setOnCheckedChangeListener { _, isChecked ->
            // Update all queued items when checkbox state changes
            paymentViewModel.updateAllProcessorTypes(isChecked)
        }
        
        // Create progress dialog
        createProgressDialog()
    }
    
    private fun createProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_progress, null)
        
        // Get references to dialog views
        dialogProgressTextView = dialogView.findViewById(R.id.text_dialog_progress)
        dialogProgressBar = dialogView.findViewById(R.id.progress_bar_dialog)
        dialogEventTextView = dialogView.findViewById(R.id.text_dialog_event)
        
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    
    private fun setupButtons() {
        // Set up payment method buttons
        findViewById<Button>(R.id.btn_add_credit).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.CREDIT)
        }
        
        findViewById<Button>(R.id.btn_add_debit).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.DEBIT)
        }
        
        findViewById<Button>(R.id.btn_add_pix).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.PIX)
        }

        findViewById<Button>(R.id.btn_add_voucher).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.VOUCHER)
        }

        findViewById<Button>(R.id.btn_add_bitcoin_ln).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.LN_BITCOIN)
        }

        findViewById<Button>(R.id.btn_add_merchant_pix).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.MERCHANT_PIX)
        }

        findViewById<Button>(R.id.btn_add_cash).setOnClickListener {
            enqueuePayment(SystemPaymentMethod.CASH)
        }

        // Set up control buttons
        findViewById<Button>(R.id.btn_start_processing).setOnClickListener {
            paymentViewModel.startProcessing()
        }
        
        // Cancel all payments button
        findViewById<View>(R.id.btn_cancel_all).setOnClickListener {
            paymentViewModel.cancelAllPayments()
        }
    }
    
    private fun initializeComponents() {
        // Initialize dialog manager
        dialogManager = PaymentDialogManager(this, layoutInflater, paymentViewModel)
        
        // Initialize event handler
        eventHandler = PaymentEventHandler(
            context = this,
            dialogEventTextView = dialogEventTextView,
            onPinDisplayUpdate = { /* PIN display handled by coordinator */ }
        )
        
        // Initialize coordinator with all dependencies
        coordinator = PaymentActivityCoordinator(
            context = this,
            lifecycleScope = lifecycleScope,
            paymentViewModel = paymentViewModel,
            dialogManager = dialogManager,
            eventHandler = eventHandler,
            queueView = queueView,
            dialogProgressTextView = dialogProgressTextView,
            dialogProgressBar = dialogProgressBar,
            dialogEventTextView = dialogEventTextView,
            showProgressDialog = { showProgressDialog() },
            hideProgressDialog = { hideProgressDialog() }
        )
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
     * Enqueue a payment with the specified method and processor type
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
            isTransactionless = paymentData.isTransactionless
        )
    }
}