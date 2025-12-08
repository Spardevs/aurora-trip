package br.com.ticpass.pos.presentation.refund

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.config.ProcessorStartMode
import br.com.ticpass.pos.core.queue.config.PersistenceStrategy
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.presentation.refund.states.RefundAction
import br.com.ticpass.pos.presentation.refund.states.RefundReducer
import br.com.ticpass.pos.presentation.refund.states.RefundUiEvent
import br.com.ticpass.pos.presentation.refund.states.RefundUiState
import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.processors.refund.utils.RefundQueueFactory
import br.com.ticpass.pos.core.queue.processors.refund.data.RefundStorage
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.presentation.refund.states.RefundSideEffect
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.PrinterNetworkInfo
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.RefundProcessorType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Refund Processing ViewModel
 * Example ViewModel that demonstrates handling input requests from refund processors
 * 
 * This ViewModel uses a modular architecture with:
 * - Actions: Represent user interactions and events
 * - Reducer: Handles state transitions and side effects
 * - RefundUiState: Represents the current UI state
 * - RefundUiEvent: Represents one-time events to be consumed by the UI
 * - RefundSideEffect: Represents operations that don't directly affect the UI state
 */
@HiltViewModel
class RefundViewModel @Inject constructor(
    refundQueueFactory: RefundQueueFactory,
    processingRefundStorage: RefundStorage,
    private val reducer: RefundReducer
) : ViewModel() {
    
    // Queue Setup and Configuration
    // Initialize the queue with viewModelScope
    private val refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent> = refundQueueFactory.createDynamicRefundQueue(
        storage = processingRefundStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode = ProcessorStartMode.IMMEDIATE,
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
                updateState(RefundUiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }
    
    // Expose queue states to UI
    val queueState = refundQueue.queueState
    val fullSize = refundQueue.fullSize
    val enqueuedSize = refundQueue.enqueuedSize
    val currentIndex = refundQueue.currentIndex
    val processingState = refundQueue.processingState
    val processingRefundEvents: SharedFlow<RefundEvent> = refundQueue.processorEvents
    
    // UI State Management
    // UI Events flow for one-time events
    private val _uiEvents = MutableSharedFlow<RefundUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    /**
     * Emit a one-time UI event
     */
    private fun emitUiEvent(event: RefundUiEvent) {
        launchInViewModelScope {
            _uiEvents.emit(event)
        }
    }
    
    // UI State flow for persistent state
    private val _uiState = MutableStateFlow<RefundUiState>(RefundUiState.Idle)
    val uiState: StateFlow<RefundUiState> = _uiState.asStateFlow()
    
    /**
     * Update the UI state
     */
    private fun updateState(newState: RefundUiState) {
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
            refundQueue.processor.userInputRequests.collect { request ->
                Log.d("RefundViewModel", "Processor input request received: ${request::class.simpleName}")
                dispatch(RefundAction.ProcessorInputRequested(request))
            }
        }
    }
    
    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: RefundAction) {
        val sideEffect = reducer.reduce(action, refundQueue)
        sideEffect?.let { executeSideEffect(it) }
    }

    /**
     * Execute a side effect
     * All side effects are executed in the ViewModel scope
     */
    private fun executeSideEffect(sideEffect: RefundSideEffect) {
        launchInViewModelScope {
            sideEffect.scope()
        }
    }
    
    // Initialization and Event Handling
    
    init {
        // Observe processing state changes
        launchInViewModelScope {
            refundQueue.processingState.collectLatest { state ->
                dispatch(RefundAction.ProcessingStateChanged(state))
            }
        }
        
        // Observe queue input requests
        launchInViewModelScope {
            refundQueue.queueInputRequests.collectLatest { request ->
                dispatch(RefundAction.QueueInputRequested(request))
            }
        }
    }
    
    // Public API
    
    /**
     * Start processing the refund queue
     */
    fun startProcessing() {
        dispatch(RefundAction.StartProcessing)
    }
    
    /**
     * Process a refund with the specified processor type
     * Uses the processor type from the mapper or the provided override
     */
    fun enqueueRefund(
        atk: String,
        txId: String,
        isQRCode: Boolean,
        processorType: RefundProcessorType
    ) {
        dispatch(RefundAction.EnqueueRefund(atk, txId, isQRCode, processorType))
    }
    
    /**
     * Cancel a refund
     */
    fun cancelRefund(refundId: String) {
        dispatch(RefundAction.CancelRefund(refundId))
    }
    
    /**
     * Cancel all refunds
     * Uses a single operation to remove all items at once
     */
    fun cancelAllRefunds() {
        dispatch(RefundAction.ClearQueue)
    }

    /**
     * Abort the current processor
     */
    fun abortRefund() {
        dispatch(RefundAction.AbortCurrentRefund)
    }
    
    // Processor-Level Input Handling

    /**
     * Confirm printer network info (processor-level input request)
     */
    fun confirmPrinterNetworkInfo(requestId: String, networkInfo: PrinterNetworkInfo) {
        dispatch(RefundAction.ConfirmPrinterNetworkInfo(requestId, networkInfo))
    }
    
    // Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun <T: QueueItem> confirmProcessor(requestId: String, modifiedItem: T) {
        dispatch(RefundAction.ConfirmProcessor(requestId, modifiedItem))
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        dispatch(RefundAction.SkipProcessor(requestId))
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(requestId: String) {
        dispatch(RefundAction.SkipProcessorOnError(requestId))
    }
    
    // Error Handling
    
    /**
     * Handle a failed refund with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    private fun handleFailedRefund(requestId: String, action: ErrorHandlingAction) {
        dispatch(RefundAction.HandleFailedRefund(requestId, action))
    }
    
    /**
     * Retry a failed refund immediately (queue-level input request)
     * This will retry the same processor without moving the item
     */
    fun retryRefund(requestId: String) {
        handleFailedRefund(requestId, ErrorHandlingAction.RETRY)
    }
    
    /**
     * Retry a failed refund later (queue-level input request)
     * This will move the item to the end of the queue and continue with the next item
     */
    fun skipRefund(requestId: String) {
        handleFailedRefund(requestId, ErrorHandlingAction.SKIP)
    }
    
    /**
     * Abort all processors and stop processing (queue-level input request)
     */
    fun abortAllRefunds(requestId: String) {
        handleFailedRefund(requestId, ErrorHandlingAction.ABORT_ALL)
    }
}
