package br.com.ticpass.pos.presentation.refund.coordination

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import br.com.ticpass.Constants.CONVERSION_FACTOR
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.refund.RefundViewModel
import br.com.ticpass.pos.presentation.refund.states.RefundUiState
import br.com.ticpass.pos.presentation.refund.dialogs.RefundDialogManager
import br.com.ticpass.pos.presentation.refund.events.RefundEventHandler
import br.com.ticpass.pos.core.refund.models.SystemRefundMethod
import br.com.ticpass.pos.presentation.refund.utils.RefundUIUtils
import br.com.ticpass.pos.presentation.refund.view.RefundQueueView
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.core.queue.models.ProcessingState
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.PrinterNetworkInfo
import br.com.ticpass.utils.toMoneyAsDouble
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates all ViewModel observations and UI updates for the RefundActivity.
 * This class handles the complex coordination logic, allowing the activity to focus on
 * lifecycle management and basic view setup.
 */
class RefundActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val refundViewModel: RefundViewModel,
    private val dialogManager: RefundDialogManager,
    private val eventHandler: RefundEventHandler,
    private val queueView: RefundQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val dialogRefundMethodTextView: TextView,
    private val showProgressDialog: () -> Unit,
    private val hideProgressDialog: () -> Unit
) {
    
    private var totalRefunds = 0
    private var currentProcessingIndex = 0
    
    /**
     * Initialize all ViewModel observations and UI coordination
     */
    fun initialize() {
        observeQueueState()
        observeUiEvents()
        observeProcessingState()
        observeRefundEvents()
        observeUiState()
    }
    
    private fun observeQueueState() {
        lifecycleScope.launch {
            refundViewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems)
            }
        }
    }
    
    private fun observeUiEvents() {
        lifecycleScope.launch {
            refundViewModel.uiEvents.collect { event ->
                eventHandler.handleUiEvent(event)
            }
        }
    }
    
    private fun observeProcessingState() {
        lifecycleScope.launch {
            refundViewModel.processingState.collectLatest { state ->

                // Show dialog for active processing states
                if (state is ProcessingState.ItemProcessing || 
                    state is ProcessingState.ItemRetrying) {
                    showProgressDialog()
                }
                
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        updateProcessingProgress(refundViewModel.currentIndex, refundViewModel.fullSize)
                        // Update refund method and amount information
                        updateRefundInfo(state.item)
                    }
                    is ProcessingState.ItemDone -> {
                        // Success: All refunds completed
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
                        updateProcessingProgress(refundViewModel.currentIndex, refundViewModel.fullSize)
                        // Update refund method and amount information for retry
                        updateRefundInfo(state.item)
                    }
                    is ProcessingState.ItemSkipped -> {
                        // Update progress for skipped state
                        updateProcessingProgress(refundViewModel.currentIndex, refundViewModel.fullSize)
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
    
    private fun observeRefundEvents() {
        lifecycleScope.launch {
            refundViewModel.processingRefundEvents.collectLatest { event ->
                eventHandler.handleRefundEvent(event)
            }
        }
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            refundViewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is RefundUiState.Error -> {
                        displayErrorMessage(uiState.event)
                    }
                    is RefundUiState.ConfirmNextProcessor<*> -> {
                        dialogManager.showConfirmNextRefundProcessorDialog(uiState.requestId)
                    }
                    is RefundUiState.ErrorRetryOrSkip -> {
                        dialogManager.showErrorRetryOptionsDialog(uiState.requestId, uiState.error)
                    }
                    is RefundUiState.ConfirmPrinterNetworkInfo -> {
                        confirmPrinterNetworkInfo(uiState.requestId)
                    }
                    else -> {
                        // Other UI states don't need dialogs
                        Log.d("RefundActivityCoordinator", "No dialog needed for state: $uiState")
                    }
                }
            }
        }
    }
    
    private fun updateQueueUI(queueItems: List<RefundQueueItem>) {
        queueView.updateQueue(queueItems)
        totalRefunds = queueItems.size
        
        // Update the queue title with item count
        val formattedTitle = String.format(context.getString(R.string.refund_queue), queueItems.size)
        queueTitleTextView.text = formattedTitle
    }
    
    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current
        
        // Update dialog progress
        if(total == 1) {
            dialogProgressTextView.text = context.getString(R.string.refund_progress_first)
        } else {
            dialogProgressTextView.text = context.getString(R.string.refund_progress, current, total)
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
        val errorMessage = RefundUIUtils.getErrorMessage(context, error)
        RefundUIUtils.logError("RefundActivityCoordinator", error, context)
        
        // Update dialog with error message
        dialogEventTextView.text = errorMessage
        
        // Make sure dialog is showing for errors
        showProgressDialog()
    }
    
    private fun updateRefundInfo(item: RefundQueueItem) {
        // Update refund method
        val refundMethod = SystemRefundMethod.entries.find { it.toString() == item.processorType.toString() }
            ?: throw IllegalArgumentException("Unknown refund method: ${item.processorType}")
        val refundMethodDisplayName = getRefundMethodDisplayName(refundMethod)
        dialogRefundMethodTextView.text = refundMethodDisplayName
    }
    
    private fun getRefundMethodDisplayName(method: SystemRefundMethod): String {
        return when (method) {
            SystemRefundMethod.ACQUIRER -> context.getString(R.string.enqueue_acquirer_refund)
        }
    }

    private fun confirmPrinterNetworkInfo(requestId: String) {
        val networkInfo = PrinterNetworkInfo("192.168.0.3", 9100)

        // Hardcoded network info for demonstration purposes.
        // In real applications, we'd typically request this from database.
        // This info could be obtained by using PrinterDiscovery utility.
        refundViewModel.confirmPrinterNetworkInfo(requestId, networkInfo)
    }
}
