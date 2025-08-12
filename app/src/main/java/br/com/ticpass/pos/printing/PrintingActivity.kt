package br.com.ticpass.pos.printing

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
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.printing.coordination.PrintingActivityCoordinator
import br.com.ticpass.pos.printing.dialogs.PrintingDialogManager
import br.com.ticpass.pos.printing.events.PrintingEventHandler
import br.com.ticpass.pos.printing.models.SupportedPrintingMethods
import br.com.ticpass.pos.printing.models.SystemPrintingMethod
import br.com.ticpass.pos.printing.utils.PrintingUIUtils
import br.com.ticpass.pos.printing.view.PrintingQueueView
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import br.com.ticpass.pos.sdk.AcquirerSdk
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrintingActivity : AppCompatActivity() {
    
    // ViewModel injected via Hilt
    private val printingViewModel: PrintingViewModel by viewModels()
    
    // Specialized components for handling different responsibilities
    private lateinit var dialogManager: PrintingDialogManager
    private lateinit var eventHandler: PrintingEventHandler
    private lateinit var coordinator: PrintingActivityCoordinator
    
    // UI components
    private lateinit var queueView: PrintingQueueView
    
    // Progress Dialog components
    private var progressDialog: AlertDialog? = null
    private lateinit var dialogProgressTextView: TextView
    private lateinit var dialogProgressBar: ProgressBar
    private lateinit var dialogEventTextView: TextView
    private lateinit var dialogPrintingMethodTextView: TextView
    private lateinit var dialogCancelButton: Button
    private lateinit var queueTitleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize SDK
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printing_processing)
        
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
        queueView = findViewById(R.id.printing_queue_view)
        
        // Set up printing queue view cancel callback
        queueView.onPrintingCanceled = { printingId ->
            printingViewModel.cancelPrinting(printingId)
        }
        // Create progress dialog
        createProgressDialog()
    }
    
    private fun createProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_printing_progress, null)
        
        // Get references to dialog views
        dialogProgressTextView = dialogView.findViewById(R.id.text_dialog_progress)
        dialogProgressBar = dialogView.findViewById(R.id.progress_bar_dialog)
        dialogEventTextView = dialogView.findViewById(R.id.text_dialog_event)
        dialogPrintingMethodTextView = dialogView.findViewById(R.id.text_printing_processor_type)
        dialogCancelButton = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        queueTitleTextView = findViewById<TextView>(R.id.text_printing_queue_title)

        dialogCancelButton.setOnClickListener {
            // Handle cancel button click
            printingViewModel.abortPrinting()
        }
        
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    
    private fun setupButtons() {
        // Generate printing method buttons dynamically based on supported methods
        generatePrintingMethodButtons()
        
        // Set up control buttons
        findViewById<Button>(R.id.btn_start_processing).setOnClickListener {
            printingViewModel.startProcessing()
        }
        
        // Cancel all printings button
        findViewById<View>(R.id.clear_list).setOnClickListener {
            printingViewModel.cancelAllPrintings()
        }
    }
    
    private fun generatePrintingMethodButtons() {
        val container = findViewById<LinearLayout>(R.id.printing_methods_container)
        val supportedMethods = SupportedPrintingMethods.methods
        
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
            
            // Create button for printing method
            val button = Button(this).apply {
                text = getPrintingMethodDisplayName(method)
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
                    enqueuePrinting(method)
                }
            }
            
            currentRow?.addView(button)
        }
    }
    
    private fun getPrintingMethodDisplayName(method: SystemPrintingMethod): String {
        return when (method) {
            SystemPrintingMethod.ACQUIRER -> getString(R.string.enqueue_acquirer_printing)
            SystemPrintingMethod.MP_4200_HS -> getString(R.string.enqueue_mp4200HS_printing)
            SystemPrintingMethod.MPT_II -> getString(R.string.enqueue_mptII_printing)
        }
    }
    
    private fun initializeComponents() {
        // Initialize dialog manager
        dialogManager = PrintingDialogManager(this, layoutInflater, printingViewModel)
        
        // Initialize event handler
        eventHandler = PrintingEventHandler(
            context = this,
            dialogEventTextView = dialogEventTextView,
        )
        
        // Initialize coordinator with all dependencies
        coordinator = PrintingActivityCoordinator(
            context = this,
            lifecycleScope = lifecycleScope,
            printingViewModel = printingViewModel,
            dialogManager = dialogManager,
            eventHandler = eventHandler,
            queueView = queueView,
            queueTitleTextView = queueTitleTextView,
            dialogProgressTextView = dialogProgressTextView,
            dialogProgressBar = dialogProgressBar,
            dialogEventTextView = dialogEventTextView,
            dialogPrintingMethodTextView = dialogPrintingMethodTextView,
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
     * Enqueue a printing with the specified method and processor type
     */
    private fun enqueuePrinting(
        processorType: SystemPrintingMethod
    ) {
        val processorType = PrintingProcessorType.entries.find { it.name == processorType.name }
            ?: throw IllegalArgumentException("Unsupported printing method: ${processorType.name}")
        val printingData = PrintingUIUtils.createPrintingData(
            "",
            processorType = processorType
        )

        printingViewModel.enqueuePrinting(
            filePath = printingData.filePath,
            processorType = printingData.processorType
        )
    }
}