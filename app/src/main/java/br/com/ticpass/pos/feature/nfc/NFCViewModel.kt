package br.com.ticpass.pos.feature.nfc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.feature.nfc.state.NFCAction
import br.com.ticpass.pos.feature.nfc.state.NFCReducer
import br.com.ticpass.pos.feature.nfc.state.NFCUiEvent
import br.com.ticpass.pos.feature.nfc.state.NFCUiState
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCQueueFactory
import br.com.ticpass.pos.queue.processors.nfc.data.NFCStorage
import br.com.ticpass.pos.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.feature.nfc.state.NFCSideEffect
import br.com.ticpass.pos.nfc.models.NFCTagCustomerDataInput
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.queue.processors.nfc.models.NFCBruteForce
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * NFC Processing ViewModel
 * Example ViewModel that demonstrates handling input requests from nfc processors
 * 
 * This ViewModel uses a modular architecture with:
 * - Actions: Represent user interactions and events
 * - Reducer: Handles state transitions and side effects
 * - NFCUiState: Represents the current UI state
 * - NFCUiEvent: Represents one-time events to be consumed by the UI
 * - NFCSideEffect: Represents operations that don't directly affect the UI state
 */
@HiltViewModel
class NFCViewModel @Inject constructor(
    nfcQueueFactory: NFCQueueFactory,
    processingNFCStorage: NFCStorage,
    private val reducer: NFCReducer
) : ViewModel() {
    
    // Queue Setup and Configuration
    // Initialize the queue with viewModelScope
    private val nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent> = nfcQueueFactory.createDynamicNFCQueue(
        storage = processingNFCStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode = ProcessorStartMode.CONFIRMATION,
        scope = viewModelScope
    )
    
    /**
     * Helper function to launch coroutines in the viewModelScope with standard error handling
     */
    private fun launchInViewModelScope(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                // Common error handling
                updateState(NFCUiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }
    
    // Expose queue states to UI
    val queueState = nfcQueue.queueState
    val fullSize = nfcQueue.fullSize
    val enqueuedSize = nfcQueue.enqueuedSize
    val currentIndex = nfcQueue.currentIndex
    val processingState = nfcQueue.processingState
    val processingNFCEvents: SharedFlow<NFCEvent> = nfcQueue.processorEvents
    
    // UI State Management
    // UI Events flow for one-time events
    private val _uiEvents = MutableSharedFlow<NFCUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    /**
     * Emit a one-time UI event
     */
    private fun emitUiEvent(event: NFCUiEvent) {
        launchInViewModelScope {
            _uiEvents.emit(event)
        }
    }
    
    // UI State flow for persistent state
    private val _uiState = MutableStateFlow<NFCUiState>(NFCUiState.Idle)
    val uiState: StateFlow<NFCUiState> = _uiState.asStateFlow()
    
    /**
     * Update the UI state
     */
    private fun updateState(newState: NFCUiState) {
        _uiState.value = newState
    }
    
    // Reducer for handling actions and producing side effects (injected)
    init {
        // Initialize the reducer with callback functions
        reducer.initialize(
            emitUiEvent = ::emitUiEvent,
            updateState = ::updateState
        )
        
        // Observe processor input requests
        viewModelScope.launch {
            nfcQueue.processor.userInputRequests.collect { request ->
                Log.d("NFCViewModel", "Processor input request received: ${request::class.simpleName}")
                dispatch(NFCAction.ProcessorInputRequested(request))
            }
        }
    }
    
    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: NFCAction) {
        val sideEffect = reducer.reduce(action, nfcQueue)
        sideEffect?.let { executeSideEffect(it) }
    }

    /**
     * Execute a side effect
     * All side effects are executed in the ViewModel scope
     */
    private fun executeSideEffect(sideEffect: NFCSideEffect) {
        launchInViewModelScope {
            sideEffect.scope()
        }
    }
    
    // Initialization and Event Handling
    
    init {
        // Observe processing state changes
        launchInViewModelScope {
            nfcQueue.processingState.collectLatest { state ->
                dispatch(NFCAction.ProcessingStateChanged(state))
            }
        }
        
        // Observe queue input requests
        launchInViewModelScope {
            nfcQueue.queueInputRequests.collectLatest { request ->
                dispatch(NFCAction.QueueInputRequested(request))
            }
        }
    }
    
    // Public API
    
    /**
     * Start processing the nfc queue
     */
    fun startProcessing() {
        dispatch(NFCAction.StartProcessing)
    }
    
    fun enqueueAuthOperation(
        timeout: Long = 15000L
    ) {
        dispatch(NFCAction.EnqueueTypedNFC(
            NFCQueueItem.CustomerAuthOperation(
                timeout = timeout
            )
        ))
    }

    fun enqueueFormatOperation(
        bruteForce: NFCBruteForce
    ) {
        dispatch(NFCAction.EnqueueTypedNFC(
            NFCQueueItem.TagFormatOperation(
                bruteForce = bruteForce
            )
        ))
    }
    
    fun enqueueSetupOperation(
        timeout: Long = 20000L
    ) {
        dispatch(NFCAction.EnqueueTypedNFC(
            NFCQueueItem.CustomerSetupOperation(
                timeout = timeout
            )
        ))
    }
    
    /**
     * Cancel a nfc
     */
    fun cancelNFC(nfcId: String) {
        dispatch(NFCAction.CancelNFC(nfcId))
    }
    
    /**
     * Cancel all nfcs
     * Uses a single operation to remove all items at once
     */
    fun cancelAllNFCs() {
        dispatch(NFCAction.ClearQueue)
    }

    /**
     * Abort the current processor
     */
    fun abortNFC() {
        dispatch(NFCAction.AbortCurrentNFC)
    }

    // Processor-Level Input Handling

    /**
     * Confirm merchant PIX has been paid
     */
    fun confirmNFCTagAuth(requestId: String, didAuth: Boolean) {
        dispatch(NFCAction.ConfirmNFCTagAuth(requestId, didAuth))
    }

    /**
     * Confirm customer data input
     */
    fun confirmNFCCustomerData(requestId: String, data: NFCTagCustomerDataInput?) {
        dispatch(NFCAction.ConfirmNFCCustomerData(requestId, data))
    }

    /**
     * Confirm that customer has saved their NFC Tag PIN
     */
    fun confirmNFCCustomerSavePin(requestId: String, didSave: Boolean) {
        dispatch(NFCAction.ConfirmNFCCustomerSavePin(requestId, didSave))
    }
    
    // Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun <T: QueueItem> confirmProcessor(requestId: String, modifiedItem: T) {
        dispatch(NFCAction.ConfirmProcessor(requestId, modifiedItem))
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        dispatch(NFCAction.SkipProcessor(requestId))
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(requestId: String) {
        dispatch(NFCAction.SkipProcessorOnError(requestId))
    }
    
    // Error Handling
    
    /**
     * Handle a failed nfc with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    private fun handleFailedNFC(requestId: String, action: ErrorHandlingAction) {
        dispatch(NFCAction.HandleFailedNFC(requestId, action))
    }
    
    /**
     * Retry a failed nfc immediately (queue-level input request)
     * This will retry the same processor without moving the item
     */
    fun retryNFC(requestId: String) {
        handleFailedNFC(requestId, ErrorHandlingAction.RETRY)
    }
    
    /**
     * Retry a failed nfc later (queue-level input request)
     * This will move the item to the end of the queue and continue with the next item
     */
    fun skipNFC(requestId: String) {
        handleFailedNFC(requestId, ErrorHandlingAction.SKIP)
    }
    
    /**
     * Abort all processors and stop processing (queue-level input request)
     */
    fun abortAllNFCs(requestId: String) {
        handleFailedNFC(requestId, ErrorHandlingAction.ABORT_ALL)
    }

    /**
     * Confirm NFC keys (processor-level input request)
     */
    fun confirmNFCKeys(requestId: String, keys: Map<NFCTagSectorKeyType, String>) {
        dispatch(NFCAction.ConfirmNFCKeys(requestId, keys))
    }
}
