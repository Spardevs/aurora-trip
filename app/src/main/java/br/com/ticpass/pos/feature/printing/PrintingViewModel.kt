package br.com.ticpass.pos.feature.printing

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
import br.com.ticpass.pos.feature.printing.state.PrintingAction
import br.com.ticpass.pos.feature.printing.state.PrintingReducer
import br.com.ticpass.pos.feature.printing.state.PrintingUiEvent
import br.com.ticpass.pos.feature.printing.state.PrintingUiState
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.printing.utils.PrintingQueueFactory
import br.com.ticpass.pos.queue.processors.printing.data.PrintingStorage
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.feature.printing.state.PrintingSideEffect
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrinterNetworkInfo
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Printing Processing ViewModel
 * Example ViewModel that demonstrates handling input requests from printing processors
 * 
 * This ViewModel uses a modular architecture with:
 * - Actions: Represent user interactions and events
 * - Reducer: Handles state transitions and side effects
 * - PrintingUiState: Represents the current UI state
 * - PrintingUiEvent: Represents one-time events to be consumed by the UI
 * - PrintingSideEffect: Represents operations that don't directly affect the UI state
 */
@HiltViewModel
class PrintingViewModel @Inject constructor(
    printingQueueFactory: PrintingQueueFactory,
    processingPrintingStorage: PrintingStorage,
    private val reducer: PrintingReducer
) : ViewModel() {
    
    // Queue Setup and Configuration
    // Initialize the queue with viewModelScope
    private val printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent> = printingQueueFactory.createDynamicPrintingQueue(
        storage = processingPrintingStorage,
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
                updateState(PrintingUiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }
    
    // Expose queue states to UI
    val queueState = printingQueue.queueState
    val fullSize = printingQueue.fullSize
    val enqueuedSize = printingQueue.enqueuedSize
    val currentIndex = printingQueue.currentIndex
    val processingState = printingQueue.processingState
    val processingPrintingEvents: SharedFlow<PrintingEvent> = printingQueue.processorEvents
    
    // UI State Management
    // UI Events flow for one-time events
    private val _uiEvents = MutableSharedFlow<PrintingUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    /**
     * Emit a one-time UI event
     */
    private fun emitUiEvent(event: PrintingUiEvent) {
        launchInViewModelScope {
            _uiEvents.emit(event)
        }
    }
    
    // UI State flow for persistent state
    private val _uiState = MutableStateFlow<PrintingUiState>(PrintingUiState.Idle)
    val uiState: StateFlow<PrintingUiState> = _uiState.asStateFlow()
    
    /**
     * Update the UI state
     */
    private fun updateState(newState: PrintingUiState) {
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
            printingQueue.processor.userInputRequests.collect { request ->
                Log.d("PrintingViewModel", "Processor input request received: ${request::class.simpleName}")
                dispatch(PrintingAction.ProcessorInputRequested(request))
            }
        }
    }
    
    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: PrintingAction) {
        val sideEffect = reducer.reduce(action, printingQueue)
        sideEffect?.let { executeSideEffect(it) }
    }

    /**
     * Execute a side effect
     * All side effects are executed in the ViewModel scope
     */
    private fun executeSideEffect(sideEffect: PrintingSideEffect) {
        launchInViewModelScope {
            sideEffect.scope()
        }
    }
    
    // Initialization and Event Handling
    
    init {
        // Observe processing state changes
        launchInViewModelScope {
            printingQueue.processingState.collectLatest { state ->
                dispatch(PrintingAction.ProcessingStateChanged(state))
            }
        }
        
        // Observe queue input requests
        launchInViewModelScope {
            printingQueue.queueInputRequests.collectLatest { request ->
                dispatch(PrintingAction.QueueInputRequested(request))
            }
        }
    }
    
    // Public API
    
    /**
     * Start processing the printing queue
     */
    fun startProcessing() {
        dispatch(PrintingAction.StartProcessing)
    }
    
    /**
     * Process a printing with the specified processor type
     * Uses the processor type from the mapper or the provided override
     */
    fun enqueuePrinting(
        filePath: String,
        processorType: PrintingProcessorType
    ) {
        dispatch(PrintingAction.EnqueuePrinting(filePath, processorType))
    }
    
    /**
     * Cancel a printing
     */
    fun cancelPrinting(printingId: String) {
        dispatch(PrintingAction.CancelPrinting(printingId))
    }
    
    /**
     * Cancel all printings
     * Uses a single operation to remove all items at once
     */
    fun cancelAllPrintings() {
        dispatch(PrintingAction.ClearQueue)
    }

    /**
     * Abort the current processor
     */
    fun abortPrinting() {
        dispatch(PrintingAction.AbortCurrentPrinting)
    }
    
    // Processor-Level Input Handling

    /**
     * Confirm printer network info (processor-level input request)
     */
    fun confirmPrinterNetworkInfo(requestId: String, networkInfo: PrinterNetworkInfo) {
        dispatch(PrintingAction.ConfirmPrinterNetworkInfo(requestId, networkInfo))
    }
    
    /**
     * Confirm printer paper cut (processor-level input request)
     */
    fun confirmPrinterPaperCut(requestId: String, paperCutType: br.com.ticpass.pos.queue.processors.printing.models.PaperCutType) {
        dispatch(PrintingAction.ConfirmPrinterPaperCut(requestId, paperCutType))
    }
    
    // Queue-Level Input Handling
    
    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun <T: QueueItem> confirmProcessor(requestId: String, modifiedItem: T) {
        dispatch(PrintingAction.ConfirmProcessor(requestId, modifiedItem))
    }
    
    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        dispatch(PrintingAction.SkipProcessor(requestId))
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(requestId: String) {
        dispatch(PrintingAction.SkipProcessorOnError(requestId))
    }
    
    // Error Handling
    
    /**
     * Handle a failed printing with the specified action (queue-level input request)
     * 
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    private fun handleFailedPrinting(requestId: String, action: ErrorHandlingAction) {
        dispatch(PrintingAction.HandleFailedPrinting(requestId, action))
    }
    
    /**
     * Retry a failed printing immediately (queue-level input request)
     * This will retry the same processor without moving the item
     */
    fun retryPrinting(requestId: String) {
        handleFailedPrinting(requestId, ErrorHandlingAction.RETRY)
    }
    
    /**
     * Retry a failed printing later (queue-level input request)
     * This will move the item to the end of the queue and continue with the next item
     */
    fun skipPrinting(requestId: String) {
        handleFailedPrinting(requestId, ErrorHandlingAction.SKIP)
    }
    
    /**
     * Abort all processors and stop processing (queue-level input request)
     */
    fun abortAllPrintings(requestId: String) {
        handleFailedPrinting(requestId, ErrorHandlingAction.ABORT_ALL)
    }
}
