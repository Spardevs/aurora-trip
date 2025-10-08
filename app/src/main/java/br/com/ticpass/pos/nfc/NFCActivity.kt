package br.com.ticpass.pos.nfc

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.nfc.NFCViewModel
import br.com.ticpass.pos.nfc.coordination.NFCActivityCoordinator
import br.com.ticpass.pos.nfc.dialogs.NFCDialogManager
import br.com.ticpass.pos.nfc.events.NFCEventHandler
import br.com.ticpass.pos.nfc.models.CartOperation
import br.com.ticpass.pos.nfc.models.SupportedNFCMethods
import br.com.ticpass.pos.nfc.models.SystemNFCMethod
import br.com.ticpass.pos.nfc.view.NFCQueueView
import br.com.ticpass.pos.nfc.view.TimeoutCountdownView
import br.com.ticpass.pos.queue.processors.nfc.models.NFCBruteForce
import br.com.ticpass.pos.sdk.AcquirerSdk
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NFCActivity : AppCompatActivity() {
    
    // ViewModel injected via Hilt
    private val nfcViewModel: NFCViewModel by viewModels()
    
    // Specialized components for handling different responsibilities
    private lateinit var dialogManager: NFCDialogManager
    private lateinit var eventHandler: NFCEventHandler
    private lateinit var coordinator: NFCActivityCoordinator
    
    // UI components
    private lateinit var queueView: NFCQueueView
    
    // Progress Dialog components
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: ProgressBar
    private lateinit var dialogEventTextView: TextView
    private lateinit var dialogNFCMethodTextView: TextView
    private lateinit var dialogCancelButton: Button
    private lateinit var queueTitleTextView: TextView
    private lateinit var dialogTimeoutCountdownView: TimeoutCountdownView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize SDK
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        
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
        queueView = findViewById(R.id.nfc_queue_view)
        
        // Set up nfc queue view cancel callback
        queueView.onNFCCanceled = { nfcId ->
            nfcViewModel.cancelNFC(nfcId)
        }
        // Create progress dialog
        createProgressDialog()
    }
    
    private fun createProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nfc_progress, null)
        
        // Get references to dialog views
        dialogProgressTextView = dialogView.findViewById(R.id.text_dialog_progress)
        dialogProgressBar = dialogView.findViewById(R.id.progress_bar_dialog)
        dialogEventTextView = dialogView.findViewById(R.id.text_dialog_event)
        dialogNFCMethodTextView = dialogView.findViewById(R.id.text_nfc_processor_type)
        dialogCancelButton = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        queueTitleTextView = findViewById<TextView>(R.id.text_nfc_queue_title)
        dialogTimeoutCountdownView = dialogView.findViewById(R.id.timeout_countdown_view)

        dialogCancelButton.setOnClickListener {
            // Handle cancel button click
            nfcViewModel.abortNFC()
        }
        
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    
    private fun setupButtons() {
        // Generate nfc method buttons dynamically based on supported methods
        generateNFCMethodButtons()
        
        // Set up control buttons
        findViewById<Button>(R.id.btn_start_processing).setOnClickListener {
            nfcViewModel.startProcessing()
        }
        
        // Cancel all nfcs button
        findViewById<View>(R.id.clear_list).setOnClickListener {
            nfcViewModel.cancelAllNFCs()
        }
    }
    
    private fun generateNFCMethodButtons() {
        val container = findViewById<LinearLayout>(R.id.nfc_methods_container)
        val supportedMethods = SupportedNFCMethods.methods
        
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
            
            // Create button for nfc method
            val button = Button(this).apply {
                text = getNFCMethodDisplayName(method)
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
                    when (method) {
                        SystemNFCMethod.CART_UPDATE -> {
                            // Show dialog to get cart update parameters
                            dialogManager.showCartUpdateDialog { productId, quantity, operation ->
                                enqueueNFC(
                                    method = method,
                                    productId = productId,
                                    quantity = quantity,
                                    operation = operation,
                                )
                            }
                        }
                        else -> {
                            enqueueNFC(method)
                        }
                    }
                }
            }
            
            currentRow?.addView(button)
        }
    }
    
    private fun getNFCMethodDisplayName(method: SystemNFCMethod): String {
        return when (method) {
            SystemNFCMethod.CUSTOMER_AUTH -> getString(R.string.enqueue_auth_nfc)
            SystemNFCMethod.CUSTOMER_SETUP -> getString(R.string.enqueue_setup_nfc)
            SystemNFCMethod.TAG_FORMAT -> getString(R.string.enqueue_format_nfc)
            SystemNFCMethod.CART_READ -> getString(R.string.enqueue_cart_read_nfc)
            SystemNFCMethod.CART_UPDATE -> getString(R.string.enqueue_cart_update_nfc)
        }
    }
    
    private fun initializeComponents() {
        // Initialize dialog manager
        dialogManager = NFCDialogManager(this, layoutInflater, nfcViewModel)
        
        // Initialize event handler
        eventHandler = NFCEventHandler(
            context = this,
            dialogEventTextView = dialogEventTextView,
            dialogTimeoutCountdownView = dialogTimeoutCountdownView,

        )
        
        // Initialize coordinator with all dependencies
        coordinator = NFCActivityCoordinator(
            context = this,
            lifecycleScope = lifecycleScope,
            nfcViewModel = nfcViewModel,
            dialogManager = dialogManager,
            eventHandler = eventHandler,
            queueView = queueView,
            queueTitleTextView = queueTitleTextView,
            dialogProgressTextView = dialogProgressTextView,
            dialogProgressBar = dialogProgressBar,
            dialogEventTextView = dialogEventTextView,
            dialogNFCMethodTextView = dialogNFCMethodTextView,
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
     * Enqueue a nfc with operation-specific data based on the method type
     */
    private fun enqueueNFC(
        method: SystemNFCMethod
    ) {
        when (method) {
            SystemNFCMethod.CUSTOMER_AUTH -> {
                nfcViewModel.enqueueAuthOperation(
                    timeout = 15000L
                )
            }
            SystemNFCMethod.TAG_FORMAT -> {
                nfcViewModel.enqueueFormatOperation(
                    bruteForce = NFCBruteForce.MOST_LIKELY
                )
            }
            SystemNFCMethod.CUSTOMER_SETUP -> {
                nfcViewModel.enqueueSetupOperation(
                    timeout = 20000L
                )
            }
            SystemNFCMethod.CART_READ -> {
                nfcViewModel.enqueueCartReadOperation(
                    timeout = 15000L
                )
            }
            SystemNFCMethod.CART_UPDATE -> {}
        }
    }

    /**
     * Enqueue a nfc with operation-specific data based on the method type
     */
    private fun enqueueNFC(
        method: SystemNFCMethod,
        productId: UShort,
        quantity: UByte,
        operation: CartOperation,
    ) {
        when (method) {
            SystemNFCMethod.CART_UPDATE -> {
                nfcViewModel.enqueueCartUpdateOperation(
                    timeout = 15000L,
                    productId = productId,
                    quantity = quantity,
                    operation = operation,
                )
            }
            else -> {}
        }
    }
}