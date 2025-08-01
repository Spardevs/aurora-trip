package br.com.ticpass.pos.payment.coordination

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.payment.dialogs.PaymentDialogManager
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.PaymentProcessingQueueView
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates all ViewModel observations and UI updates for the PaymentProcessingActivity.
 * This class handles the complex coordination logic, allowing the activity to focus on
 * lifecycle management and basic view setup.
 */
class PaymentActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val paymentViewModel: PaymentProcessingViewModel,
    private val dialogManager: PaymentDialogManager,
    private val eventHandler: PaymentEventHandler,
    private val queueView: PaymentProcessingQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val showProgressDialog: () -> Unit,
    private val hideProgressDialog: () -> Unit
) {
    
    private var totalPayments = 0
    private var currentProcessingIndex = 0
    
    /**
     * Initialize all ViewModel observations and UI coordination
     */
    fun initialize() {
        observeQueueState()
        observeUiEvents()
        observeProcessingState()
        observePaymentEvents()
        observeUiState()
    }
    
    private fun observeQueueState() {
        lifecycleScope.launch {
            paymentViewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems)
            }
        }
    }
    
    private fun observeUiEvents() {
        lifecycleScope.launch {
            paymentViewModel.uiEvents.collect { event ->
                eventHandler.handleUiEvent(event)
            }
        }
    }
    
    private fun observeProcessingState() {
        lifecycleScope.launch {
            paymentViewModel.processingState.collectLatest { state ->

                // Show dialog for active processing states
                if (state is ProcessingState.ItemProcessing || 
                    state is ProcessingState.ItemRetrying) {
                    showProgressDialog()
                }
                
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        updateProcessingProgress(paymentViewModel.currentIndex, paymentViewModel.fullSize)
                    }
                    is ProcessingState.ItemDone -> {
                        // Success: All payments completed
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
                        updateProcessingProgress(paymentViewModel.currentIndex, paymentViewModel.fullSize)
                    }
                    is ProcessingState.ItemSkipped -> {
                        // Update progress for skipped state
                        updateProcessingProgress(paymentViewModel.currentIndex, paymentViewModel.fullSize)
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
    
    private fun observePaymentEvents() {
        lifecycleScope.launch {
            paymentViewModel.processingPaymentEvents.collectLatest { event ->
                eventHandler.handlePaymentEvent(event)
            }
        }
    }
    
    private fun observeUiState() {
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
                        Log.d("PaymentActivityCoordinator", "No dialog needed for state: $uiState")
                    }
                }
            }
        }
    }
    
    private fun updateQueueUI(queueItems: List<ProcessingPaymentQueueItem>) {
        queueView.updateQueue(queueItems)
        totalPayments = queueItems.size
        
        // Update the queue title with item count
        val formattedTitle = String.format(context.getString(R.string.payment_queue), queueItems.size)
        queueTitleTextView.text = formattedTitle
    }
    
    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current
        
        // Update dialog progress
        if(total == 1) {
            dialogProgressTextView.text = context.getString(R.string.payment_progress_first)
        } else {
            dialogProgressTextView.text = context.getString(R.string.payment_progress, current, total)
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
        val errorMessage = PaymentUIUtils.getErrorMessage(context, error)
        PaymentUIUtils.logError("PaymentActivityCoordinator", error, context)
        
        // Update dialog with error message
        dialogEventTextView.text = errorMessage
        
        // Make sure dialog is showing for errors
        showProgressDialog()
    }
    
    private fun confirmMerchantPixKey(requestId: String) {
        val pixKey = PaymentUIUtils.getHardcodedPixKey()
        
        // Directly confirm the PIX key without showing a dialog
        paymentViewModel.confirmMerchantPixKey(requestId, pixKey)
    }
}
