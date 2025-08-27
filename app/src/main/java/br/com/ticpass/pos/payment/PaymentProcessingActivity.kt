package br.com.ticpass.pos.payment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.coordination.PaymentActivityCoordinator
import br.com.ticpass.pos.payment.dialogs.PaymentDialogManager
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.models.SupportedPaymentMethods
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.PaymentProcessingQueueView
import br.com.ticpass.pos.sdk.AcquirerSdk
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentProcessingActivity : AppCompatActivity(), PaymentEnqueuer {
    
    // ViewModel injected via Hilt
    override val paymentViewModel: PaymentProcessingViewModel by viewModels()
    
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
    private lateinit var dialogPaymentMethodTextView: TextView
    private lateinit var dialogPaymentAmountTextView: TextView
    private lateinit var dialogQRCodeImageView: android.widget.ImageView
    private lateinit var dialogTimeoutCountdownView: br.com.ticpass.pos.payment.view.TimeoutCountdownView
    private lateinit var dialogCancelButton: Button
    private lateinit var queueTitleTextView: TextView

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
            paymentViewModel.toggleTransactionless(isChecked)
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
        dialogPaymentMethodTextView = dialogView.findViewById(R.id.text_payment_method)
        dialogPaymentAmountTextView = dialogView.findViewById(R.id.text_payment_amount)
        dialogQRCodeImageView = dialogView.findViewById(R.id.image_dialog_qrcode)
        dialogTimeoutCountdownView = dialogView.findViewById(R.id.timeout_countdown_view)
        dialogCancelButton = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        queueTitleTextView = findViewById<TextView>(R.id.text_payment_queue_title)

        dialogCancelButton.setOnClickListener {
            // Handle cancel button click
            paymentViewModel.abortPayment()
        }
        
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    
    private fun setupButtons() {
        // Generate payment method buttons dynamically based on supported methods
        generatePaymentMethodButtons()
        
        // Set up control buttons
        findViewById<Button>(R.id.btn_start_processing).setOnClickListener {
            paymentViewModel.startProcessing()
        }
        
        // Cancel all payments button
        findViewById<View>(R.id.clear_list).setOnClickListener {
            paymentViewModel.cancelAllPayments()
        }
    }

    private fun generatePaymentMethodButtons() {
        val container = findViewById<LinearLayout>(R.id.payment_methods_container)
        val supportedMethods = SupportedPaymentMethods.methods

        container.removeAllViews()

        val buttonsPerRow = 2
        var currentRow: LinearLayout? = null

        supportedMethods.forEachIndexed { index, method ->
            if (index % buttonsPerRow == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (index > 0) topMargin = resources.getDimensionPixelSize(R.dimen.button_row_margin)
                    }
                }
                container.addView(currentRow)
            }

            val button = Button(this).apply {
                text = getPaymentMethodDisplayName(method)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    val margin = resources.getDimensionPixelSize(R.dimen.button_margin)
                    if (index % buttonsPerRow == 0) {
                        rightMargin = margin / 2
                    } else {
                        leftMargin = margin / 2
                    }
                }
                setOnClickListener {
                    enqueuePayment(method) // ✅ agora funciona
                }
            }

            currentRow?.addView(button)
        }
    }

    private fun getPaymentMethodDisplayName(method: SystemPaymentMethod): String {
        return when (method) {
            SystemPaymentMethod.CREDIT -> getString(R.string.enqueue_credit_payment)
            SystemPaymentMethod.DEBIT -> getString(R.string.enqueue_debit_payment)
            SystemPaymentMethod.VOUCHER -> getString(R.string.enqueue_voucher_payment)
            SystemPaymentMethod.PIX -> getString(R.string.enqueue_pix_payment)
            SystemPaymentMethod.MERCHANT_PIX -> getString(R.string.enqueue_personal_pix_payment)
            SystemPaymentMethod.CASH -> getString(R.string.enqueue_cash_payment)
            SystemPaymentMethod.LN_BITCOIN -> getString(R.string.enqueue_bitcoin_ln_payment)
        }
    }
    
    private fun initializeComponents() {
        // Initialize dialog manager
        dialogManager = PaymentDialogManager(this, layoutInflater, paymentViewModel)
        
        // Initialize event handler
        eventHandler = PaymentEventHandler(
            context = this,
            dialogEventTextView = dialogEventTextView,
            dialogQRCodeImageView = dialogQRCodeImageView,
            dialogTimeoutCountdownView = dialogTimeoutCountdownView,
        )
        
        // Initialize coordinator with all dependencies
        coordinator = PaymentActivityCoordinator(
            context = this,
            lifecycleScope = lifecycleScope,
            paymentViewModel = paymentViewModel,
            dialogManager = dialogManager,
            eventHandler = eventHandler,
            queueView = queueView,
            queueTitleTextView = queueTitleTextView,
            dialogProgressTextView = dialogProgressTextView,
            dialogProgressBar = dialogProgressBar,
            dialogEventTextView = dialogEventTextView,
            dialogPaymentMethodTextView = dialogPaymentMethodTextView,
            dialogPaymentAmountTextView = dialogPaymentAmountTextView,
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

    override fun enqueuePayment(method: SystemPaymentMethod) {
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

interface PaymentEnqueuer {
    val paymentViewModel: PaymentProcessingViewModel
    fun enqueuePayment(method: SystemPaymentMethod) {
        Log.d("PaymentEnqueuer", "Enqueueing payment for method: $method")
        val paymentData = PaymentUIUtils.createPaymentData(
            method = method,
            isTransactionlessEnabled = false,
            amount = 100,
            commission = 0
        )

        paymentViewModel.enqueuePayment(
            amount = paymentData.amount,
            commission = paymentData.commission,
            method = paymentData.method,
            isTransactionless = paymentData.isTransactionless
        )
    }
}
