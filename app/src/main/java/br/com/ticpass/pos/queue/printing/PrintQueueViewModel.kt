package br.com.ticpass.pos.queue.printing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.QueueItemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Print Queue ViewModel
 * Example ViewModel for integrating the print queue with UI
 */
class PrintQueueViewModel(
    private val printStorage: PrintStorage
) : ViewModel() {
    
    private val queueFactory = PrintQueueFactory()
    
    // Create print queue with immediate persistence
    private val printQueue = queueFactory.createPrintQueue(
        storage = printStorage,
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope = viewModelScope
    )
    
    // Expose queue states to UI
    val printQueueState = printQueue.queueState
    val printProcessingState = printQueue.processingState
    
    // Expose printer events to UI
    val printEvents: SharedFlow<PrintingEvent> = printQueue.processorEvents
    
    // UI state for printer status
    private val _printerStatus = MutableStateFlow<PrinterStatus>(PrinterStatus.Idle)
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()
    
    init {
        // Observe print events to update printer status
        viewModelScope.launch {
            printEvents.collect { event ->
                when (event) {
                    is PrintingEvent.Started -> _printerStatus.value = PrinterStatus.Initializing
                    is PrintingEvent.ConnectingToPrinter -> _printerStatus.value = PrinterStatus.Connecting
                    is PrintingEvent.PrinterConnected -> _printerStatus.value = PrinterStatus.Connected(event.printerName)
                    is PrintingEvent.OutOfPaper -> _printerStatus.value = PrinterStatus.OutOfPaper
                    is PrintingEvent.Progress -> _printerStatus.value = PrinterStatus.Printing(event.percentComplete)
                    is PrintingEvent.Completed -> _printerStatus.value = PrinterStatus.Idle
                    is PrintingEvent.Failed -> _printerStatus.value = PrinterStatus.Error(event.error)
                    else -> {} // Handle other events if needed
                }
            }
        }
    }
    
    /**
     * Print a receipt
     */
    fun printReceipt(content: String, copies: Int = 1) {
        viewModelScope.launch {
            val printItem = PrintQueueItem(
                content = content,
                copies = copies,
                paperSize = PrintQueueItem.PaperSize.RECEIPT,
                priority = 10, // High priority for receipts
                status = QueueItemStatus.PENDING // Initial status
            )
            printQueue.enqueue(printItem)
        }
    }
    
    /**
     * Print a document
     */
    fun printDocument(content: String, paperSize: PrintQueueItem.PaperSize, copies: Int = 1) {
        viewModelScope.launch {
            val printItem = PrintQueueItem(
                content = content,
                copies = copies,
                paperSize = paperSize,
                priority = 5, // Medium priority for documents
                status = QueueItemStatus.PENDING // Initial status
            )
            printQueue.enqueue(printItem)
        }
    }
    
    /**
     * Get the current print job being processed, if any
     */
    fun getCurrentPrintJob(): PrintQueueItem? {
        val state = printProcessingState.value
        return when (state) {
            is ProcessingState.ItemProcessing -> state.item
            is ProcessingState.ItemRetrying -> state.item
            else -> null
        }
    }
    
    /**
     * Cancel a print job
     */
    fun cancelPrintJob(printItem: PrintQueueItem) {
        viewModelScope.launch {
            printQueue.remove(printItem)
        }
    }
    
    /**
     * Clear all completed print jobs from storage
     */
    fun clearCompletedPrintJobs() {
        viewModelScope.launch {
            printQueue.clearCompleted()
        }
    }
    
    /**
     * Printer status states
     */
    sealed class PrinterStatus {
        object Idle : PrinterStatus()
        object Initializing : PrinterStatus()
        object Connecting : PrinterStatus()
        data class Connected(val printerName: String) : PrinterStatus()
        data class Printing(val progress: Int) : PrinterStatus()
        object OutOfPaper : PrinterStatus()
        data class Error(val message: String) : PrinterStatus()
    }
}
