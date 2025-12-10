package br.com.ticpass.pos.presentation.refund

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.refund.RefundViewModel
import br.com.ticpass.pos.presentation.refund.coordination.RefundActivityCoordinator
import br.com.ticpass.pos.presentation.refund.dialogs.RefundDialogManager
import br.com.ticpass.pos.presentation.refund.events.RefundEventHandler
import br.com.ticpass.pos.core.refund.models.SystemRefundMethod
import br.com.ticpass.pos.presentation.refund.utils.RefundUIUtils
import br.com.ticpass.pos.presentation.refund.view.RefundQueueView
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.RefundProcessorType
import br.com.ticpass.pos.refund.models.SupportedRefundMethods
import br.com.ticpass.pos.core.sdk.AcquirerSdk
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RefundActivity : AppCompatActivity() {
    
    // ViewModel injected via Hilt
    private val refundViewModel: RefundViewModel by viewModels()
    
    // Specialized components for handling different responsibilities
    private lateinit var dialogManager: RefundDialogManager
    private lateinit var eventHandler: RefundEventHandler
    private lateinit var coordinator: RefundActivityCoordinator
    
    // UI components
    private lateinit var queueView: RefundQueueView
    private lateinit var editTextAtk: TextInputEditText
    private lateinit var editTextTxId: TextInputEditText
    private lateinit var checkboxIsQRCode: AppCompatCheckBox
    
    // Progress Dialog components
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: ProgressBar
    private lateinit var dialogEventTextView: TextView
    private lateinit var dialogRefundMethodTextView: TextView
    private lateinit var dialogCancelButton: Button
    private lateinit var queueTitleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize SDK
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refund_processing)
        
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
        queueView = findViewById(R.id.refund_queue_view)
        
        // Initialize text input fields
        editTextAtk = findViewById(R.id.edit_text_atk)
        editTextTxId = findViewById(R.id.edit_text_tx_id)
        checkboxIsQRCode = findViewById(R.id.checkbox_is_qrcode)
        
        // Set up refund queue view cancel callback
        queueView.onRefundCanceled = { refundId ->
            refundViewModel.cancelRefund(refundId)
        }
        // Create progress dialog
        createProgressDialog()
    }
    
    private fun createProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_refund_progress, null)
        
        // Get references to dialog views
        dialogProgressTextView = dialogView.findViewById(R.id.text_dialog_progress)
        dialogProgressBar = dialogView.findViewById(R.id.progress_bar_dialog)
        dialogEventTextView = dialogView.findViewById(R.id.text_dialog_event)
        dialogRefundMethodTextView = dialogView.findViewById(R.id.text_refund_processor_type)
        dialogCancelButton = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        queueTitleTextView = findViewById<TextView>(R.id.text_refund_queue_title)

        dialogCancelButton.setOnClickListener {
            // Handle cancel button click
            refundViewModel.abortRefund()
        }
        
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    
    private fun setupButtons() {
        // Generate refund method buttons dynamically based on supported methods
        generateRefundMethodButtons()
        
        // Set up control buttons
        findViewById<Button>(R.id.btn_start_processing).setOnClickListener {
            refundViewModel.startProcessing()
        }
        
        // Cancel all refunds button
        findViewById<View>(R.id.clear_list).setOnClickListener {
            refundViewModel.cancelAllRefunds()
        }
    }
    
    private fun generateRefundMethodButtons() {
        val container = findViewById<LinearLayout>(R.id.refund_methods_container)
        val supportedMethods = SupportedRefundMethods.methods
        
        // Clear any existing buttons
        container.removeAllViews()
        
        // Create buttons in rows of 2
        val buttonsPerRow = 2
        var currentRow: LinearLayout? = null
        
        supportedMethods.forEachIndexed { index, method ->
            // Create new row if needed
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
            
            // Create button for refund method
            val button = Button(this).apply {
                text = getRefundMethodDisplayName(method)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    // Add margins between buttons
                    val margin = resources.getDimensionPixelSize(R.dimen.button_margin)
                    if (index % buttonsPerRow == 0) {
                        rightMargin = margin / 2
                    } else {
                        leftMargin = margin / 2
                    }
                }
                setOnClickListener {
                    enqueueRefund(
                        atk = editTextAtk.text.toString(),
                        txId = editTextTxId.text.toString(),
                        isQRCode = checkboxIsQRCode.isChecked,
                        processorType = method
                    )
                }
            }
            
            currentRow?.addView(button)
        }
    }
    
    private fun getRefundMethodDisplayName(method: SystemRefundMethod): String {
        return when (method) {
            SystemRefundMethod.ACQUIRER -> getString(R.string.enqueue_acquirer_refund)
        }
    }
    
    private fun initializeComponents() {
        // Initialize dialog manager
        dialogManager = RefundDialogManager(this, layoutInflater, refundViewModel)
        
        // Initialize event handler
        eventHandler = RefundEventHandler(
            context = this,
            dialogEventTextView = dialogEventTextView,
        )
        
        // Initialize coordinator with all dependencies
        coordinator = RefundActivityCoordinator(
            context = this,
            lifecycleScope = lifecycleScope,
            refundViewModel = refundViewModel,
            dialogManager = dialogManager,
            eventHandler = eventHandler,
            queueView = queueView,
            queueTitleTextView = queueTitleTextView,
            dialogProgressTextView = dialogProgressTextView,
            dialogProgressBar = dialogProgressBar,
            dialogEventTextView = dialogEventTextView,
            dialogRefundMethodTextView = dialogRefundMethodTextView,
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
     * Enqueue a refund with the specified method and processor type
     */
    private fun enqueueRefund(
        atk: String,
        txId: String?,
        isQRCode: Boolean?,
        processorType: SystemRefundMethod
    ) {
        val processorType = RefundProcessorType.entries.find { it.name == processorType.name }
            ?: throw IllegalArgumentException("Unsupported refund method: ${processorType.name}")
        val refundData = RefundUIUtils.createRefundData(
            atk = atk,
            txId = txId,
            isQRCode = isQRCode,
            processorType = processorType
        )

        refundViewModel.enqueueRefund(
            atk = refundData.atk,
            txId = refundData.txId,
            isQRCode = refundData.isQRCode,
            processorType = refundData.processorType
        )
    }
}