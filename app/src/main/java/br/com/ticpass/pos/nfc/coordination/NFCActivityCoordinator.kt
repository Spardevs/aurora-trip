package br.com.ticpass.pos.nfc.coordination

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.nfc.NFCViewModel
import br.com.ticpass.pos.feature.nfc.state.NFCUiState
import br.com.ticpass.pos.nfc.dialogs.NFCDialogManager
import br.com.ticpass.pos.nfc.events.NFCEventHandler
import br.com.ticpass.pos.nfc.models.SystemNFCMethod
import br.com.ticpass.pos.nfc.utils.NFCUIUtils
import br.com.ticpass.pos.nfc.view.NFCQueueView
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.models.NFCSuccess
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates all ViewModel observations and UI updates for the NFCActivity.
 * This class handles the complex coordination logic, allowing the activity to focus on
 * lifecycle management and basic view setup.
 */
class NFCActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val nfcViewModel: NFCViewModel,
    private val dialogManager: NFCDialogManager,
    private val eventHandler: NFCEventHandler,
    private val queueView: NFCQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val dialogNFCMethodTextView: TextView,
    private val showProgressDialog: () -> Unit,
    private val hideProgressDialog: () -> Unit
) {
    
    private var totalNFCs = 0
    private var currentProcessingIndex = 0
    
    /**
     * Initialize all ViewModel observations and UI coordination
     */
    fun initialize() {
        observeQueueState()
        observeUiEvents()
        observeProcessingState()
        observeNFCEvents()
        observeUiState()
    }
    
    private fun observeQueueState() {
        lifecycleScope.launch {
            nfcViewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems)
            }
        }
    }
    
    private fun observeUiEvents() {
        lifecycleScope.launch {
            nfcViewModel.uiEvents.collect { event ->
                eventHandler.handleUiEvent(event)
            }
        }
    }
    
    private fun observeProcessingState() {
        lifecycleScope.launch {
            nfcViewModel.processingState.collectLatest { state ->

                // Show dialog for active processing states
                if (state is ProcessingState.ItemProcessing || 
                    state is ProcessingState.ItemRetrying) {
                    showProgressDialog()
                }
                
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        updateProcessingProgress(nfcViewModel.currentIndex, nfcViewModel.fullSize)
                        // Update nfc method and amount information
                        updateNFCInfo(state.item)
                    }
                    is ProcessingState.ItemDone -> {
                        // Success: All nfcs completed
                        state.result.let { result ->
                            // Handle success state
                            when (result) {
                                is NFCSuccess.CustomerAuthSuccess -> {
                                    // Handle auth success
                                    Log.i("NFCActivityCoordinator", "NFC Auth Success:" +
                                            " ${result.id} ${result.name} ${result.phone} ${result.nationalId} ${result.subjectId}")
                                }

                                is NFCSuccess.CustomerSetupSuccess -> {
                                    // Handle setup success
                                    Log.i("NFCActivityCoordinator", "✅ Customer data written successfully" +
                                            " (ID: ${result.id}, Name: ${result.name}, " +
                                            "National Id: ${result.nationalId}, Phone: ${result.phone}, " +
                                            "PIN: ${result.pin}) " +
                                            "subjectId: ${result.subjectId}")
                                }

                                is NFCSuccess.FormatSuccess -> {
                                    // Handle reset success
                                    Log.d("NFCActivityCoordinator", "NFC Reset Success")
                                }

                                is NFCSuccess.CartReadSuccess -> {
                                    // Handle cart read success
                                    Log.i("NFCActivityCoordinator", "✅ Cart read successfully: ${result.items.size} items")
                                    result.items.forEach { item ->
                                        Log.i("NFCActivityCoordinator", "   - ${item.id}, Qty: ${item.count}, Price : $${item.price.toDouble()/100.0}" )
                                    }
                                }

                                is NFCSuccess.CartUpdateSuccess -> {
                                    // Handle cart update success
                                    Log.i("NFCActivityCoordinator", "✅ Cart updated successfully: ${result.items.size} items")
                                    result.items.forEach { item ->
                                        Log.i("NFCActivityCoordinator", "   - ${item.id}, Qty: ${item.count}, Price : $${item.price.toDouble()/100.0}" )
                                    }
                                }

                                else -> {}
                            }
                        }
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
                        updateProcessingProgress(nfcViewModel.currentIndex, nfcViewModel.fullSize)
                        // Update nfc method and amount information for retry
                        updateNFCInfo(state.item)
                    }
                    is ProcessingState.ItemSkipped -> {
                        // Update progress for skipped state
                        updateProcessingProgress(nfcViewModel.currentIndex, nfcViewModel.fullSize)
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
    
    private fun observeNFCEvents() {
        lifecycleScope.launch {
            nfcViewModel.processingNFCEvents.collectLatest { event ->
                eventHandler.handleNFCEvent(event)
            }
        }
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            nfcViewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is NFCUiState.Error -> {
                        displayErrorMessage(uiState.event)
                    }
                    is NFCUiState.ConfirmNextProcessor<*> -> {
                        dialogManager.showConfirmNextNFCProcessorDialog(uiState.requestId)
                    }
                    is NFCUiState.ErrorRetryOrSkip -> {
                        dialogManager.showErrorRetryOptionsDialog(uiState.requestId, uiState.error)
                    }
                    is NFCUiState.ConfirmNFCCustomerData -> {
                        dialogManager.showConfirmNFCCustomerData(uiState.requestId, uiState.timeoutMs)
                    }
                    is NFCUiState.ConfirmNFCCustomerSavePin -> {
                        dialogManager.showConfirmNFCCustomerSavePin(
                            uiState.requestId,
                            uiState.timeoutMs,
                            uiState.pin,
                        )
                    }
                    is NFCUiState.ConfirmNFCKeys -> {
                        confirmNFCKeys(uiState.requestId)
                    }
                    is NFCUiState.ConfirmNFCTagAuth -> {
                        dialogManager.showConfirmNFCTagAuthDialog(
                            uiState.requestId,
                            uiState.timeoutMs,
                            uiState.pin,
                            uiState.subjectId
                        )
                    }
                    else -> {
                        // Other UI states don't need dialogs
                        Log.d("NFCActivityCoordinator", "No dialog needed for state: $uiState")
                    }
                }
            }
        }
    }
    
    private fun updateQueueUI(queueItems: List<NFCQueueItem>) {
        queueView.updateQueue(queueItems)
        totalNFCs = queueItems.size
        
        // Update the queue title with item count
        val formattedTitle = String.format(context.getString(R.string.nfc_queue), queueItems.size)
        queueTitleTextView.text = formattedTitle
    }
    
    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current
        
        // Update dialog progress
        if(total == 1) {
            dialogProgressTextView.text = context.getString(R.string.nfc_progress_first)
        } else {
            dialogProgressTextView.text = context.getString(R.string.nfc_progress, current, total)
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
        val errorMessage = NFCUIUtils.getErrorMessage(context, error)
        NFCUIUtils.logError("NFCActivityCoordinator", error, context)
        
        // Update dialog with error message
        dialogEventTextView.text = errorMessage
        
        // Make sure dialog is showing for errors
        showProgressDialog()
    }
    
    private fun updateNFCInfo(item: NFCQueueItem) {
        // Update nfc method
        val nfcMethod = SystemNFCMethod.entries.find { it.toString() == item.processorType.toString() }
            ?: throw IllegalArgumentException("Unknown nfc method: ${item.processorType}")
        val nfcMethodDisplayName = getNFCMethodDisplayName(nfcMethod)
        dialogNFCMethodTextView.text = nfcMethodDisplayName
    }
    
    private fun getNFCMethodDisplayName(method: SystemNFCMethod): String {
        return when (method) {
            SystemNFCMethod.CUSTOMER_AUTH -> context.getString(R.string.enqueue_auth_nfc)
            SystemNFCMethod.CUSTOMER_SETUP -> context.getString(R.string.enqueue_setup_nfc)
            SystemNFCMethod.TAG_FORMAT -> context.getString(R.string.enqueue_format_nfc)
            SystemNFCMethod.CART_READ -> context.getString(R.string.enqueue_cart_read_nfc)
            SystemNFCMethod.CART_UPDATE -> context.getString(R.string.enqueue_cart_update_nfc)
        }
    }

    private fun confirmNFCKeys(requestId: String) {
        val keys = NFCUIUtils.getNFCKeys()

        // Directly confirm the NFC keys without showing a dialog
        nfcViewModel.confirmNFCKeys(requestId, keys)
    }
}
