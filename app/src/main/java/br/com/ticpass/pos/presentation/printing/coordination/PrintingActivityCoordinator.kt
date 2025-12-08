package br.com.ticpass.pos.presentation.printing.coordination

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import br.com.ticpass.Constants.CONVERSION_FACTOR
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.printing.PrintingViewModel
import br.com.ticpass.pos.presentation.printing.states.PrintingUiState
import br.com.ticpass.pos.presentation.printing.dialogs.PrintingDialogManager
import br.com.ticpass.pos.presentation.printing.events.PrintingEventHandler
import br.com.ticpass.pos.core.printing.models.SystemPrintingMethod
import br.com.ticpass.pos.presentation.printing.utils.PrintingUIUtils
import br.com.ticpass.pos.presentation.printing.view.PrintingQueueView
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.core.queue.models.ProcessingState
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.core.queue.processors.printing.processors.models.PrinterNetworkInfo
import br.com.ticpass.utils.toMoneyAsDouble
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates all ViewModel observations and UI updates for the PrintingActivity.
 * This class handles the complex coordination logic, allowing the activity to focus on
 * lifecycle management and basic view setup.
 */
class PrintingActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val printingViewModel: PrintingViewModel,
    private val dialogManager: PrintingDialogManager,
    private val eventHandler: PrintingEventHandler,
    private val queueView: PrintingQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val dialogPrintingMethodTextView: TextView,
    private val showProgressDialog: () -> Unit,
    private val hideProgressDialog: () -> Unit
) {
    
    private var totalPrintings = 0
    private var currentProcessingIndex = 0
    
    /**
     * Initialize all ViewModel observations and UI coordination
     */
    fun initialize() {
        observeQueueState()
        observeUiEvents()
        observeProcessingState()
        observePrintingEvents()
        observeUiState()
    }
    
    private fun observeQueueState() {
        lifecycleScope.launch {
            printingViewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems)
            }
        }
    }
    
    private fun observeUiEvents() {
        lifecycleScope.launch {
            printingViewModel.uiEvents.collect { event ->
                eventHandler.handleUiEvent(event)
            }
        }
    }
    
    private fun observeProcessingState() {
        lifecycleScope.launch {
            printingViewModel.processingState.collectLatest { state ->

                // Show dialog for active processing states
                if (state is ProcessingState.ItemProcessing || 
                    state is ProcessingState.ItemRetrying) {
                    showProgressDialog()
                }
                
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        updateProcessingProgress(printingViewModel.currentIndex, printingViewModel.fullSize)
                        // Update printing method and amount information
                        updatePrintingInfo(state.item)
                    }
                    is ProcessingState.ItemDone -> {
                        // Success: All printings completed
                    }
                    is ProcessingState.ItemFailed -> {
                        // Handle error state
                        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(state.error)
                        val displayMessage = context.getString(resourceId)

                        // Display the error in the progress area
                        displayErrorMessage(state.error)
                    }
                    is ProcessingState.ItemRetrying -> {
                        // Update progress for retrying state
                        updateProcessingProgress(printingViewModel.currentIndex, printingViewModel.fullSize)
                        // Update printing method and amount information for retry
                        updatePrintingInfo(state.item)
                    }
                    is ProcessingState.ItemSkipped -> {
                        // Update progress for skipped state
                        updateProcessingProgress(printingViewModel.currentIndex, printingViewModel.fullSize)
                    }
                    is ProcessingState.QueueCanceled -> {
                        // Reset progress when queue is canceled
                        updateProcessingProgress(0, 0)
                    }
                    is ProcessingState.QueueDone -> {
                        // Reset progress when queue processing is done
                        updateProcessingProgress(0, 0)
                    }
                    else -> { 
                        // Reset progress for other states
                        updateProcessingProgress(0, 0)
                    }
                }
            }
        }
    }
    
    private fun observePrintingEvents() {
        lifecycleScope.launch {
            printingViewModel.processingPrintingEvents.collectLatest { event ->
                eventHandler.handlePrintingEvent(event)
            }
        }
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            printingViewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is PrintingUiState.Error -> {
                        displayErrorMessage(uiState.event)
                    }
                    is PrintingUiState.ConfirmNextProcessor<*> -> {
                        dialogManager.showConfirmNextPrintingProcessorDialog(uiState.requestId)
                    }
                    is PrintingUiState.ErrorRetryOrSkip -> {
                        dialogManager.showErrorRetryOptionsDialog(uiState.requestId, uiState.error)
                    }
                    is PrintingUiState.ConfirmPrinterNetworkInfo -> {
                        confirmPrinterNetworkInfo(uiState.requestId)
                    }
                    is PrintingUiState.ConfirmPrinterPaperCut -> {
                        dialogManager.showPaperCutConfirmationDialog(uiState.requestId, uiState.timeoutMs)
                    }
                    else -> {
                        // Other UI states don't need dialogs
                        Log.d("PrintingActivityCoordinator", "No dialog needed for state: $uiState")
                    }
                }
            }
        }
    }
    
    private fun updateQueueUI(queueItems: List<PrintingQueueItem>) {
        queueView.updateQueue(queueItems)
        totalPrintings = queueItems.size
        
        // Update the queue title with item count
        val formattedTitle = String.format(context.getString(R.string.printing_queue), queueItems.size)
        queueTitleTextView.text = formattedTitle
    }
    
    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current
        
        // Update dialog progress
        if(total == 1) {
            dialogProgressTextView.text = context.getString(R.string.printing_progress_first)
        } else {
            dialogProgressTextView.text = context.getString(R.string.printing_progress, current, total)
        }
        dialogProgressBar.progress = current
        dialogProgressBar.max = total
        
        // Show dialog if processing is happening, hide otherwise
        if (total > 0) {
            showProgressDialog()
        } else {
            hideProgressDialog()
        }
    }
    
    private fun displayErrorMessage(error: ProcessingErrorEvent) {
        val errorMessage = PrintingUIUtils.getErrorMessage(context, error)
        PrintingUIUtils.logError("PrintingActivityCoordinator", error, context)
        
        // Update dialog with error message
        dialogEventTextView.text = errorMessage
        
        // Make sure dialog is showing for errors
        showProgressDialog()
    }
    
    private fun updatePrintingInfo(item: PrintingQueueItem) {
        // Update printing method
        val printingMethod = SystemPrintingMethod.entries.find { it.toString() == item.processorType.toString() }
            ?: throw IllegalArgumentException("Unknown printing method: ${item.processorType}")
        val printingMethodDisplayName = getPrintingMethodDisplayName(printingMethod)
        dialogPrintingMethodTextView.text = printingMethodDisplayName
    }
    
    private fun getPrintingMethodDisplayName(method: SystemPrintingMethod): String {
        return when (method) {
            SystemPrintingMethod.ACQUIRER -> context.getString(R.string.enqueue_acquirer_printing)
            SystemPrintingMethod.MP_4200_HS -> context.getString(R.string.enqueue_mp4200HS_printing)
            SystemPrintingMethod.MPT_II -> context.getString(R.string.enqueue_mptII_printing)
        }
    }

    private fun confirmPrinterNetworkInfo(requestId: String) {
        val networkInfo = PrinterNetworkInfo("192.168.0.3", 9100)

        // Hardcoded network info for demonstration purposes.
        // In real applications, we'd typically request this from database.
        // This info could be obtained by using PrinterDiscovery utility.
        printingViewModel.confirmPrinterNetworkInfo(requestId, networkInfo)
    }
}
