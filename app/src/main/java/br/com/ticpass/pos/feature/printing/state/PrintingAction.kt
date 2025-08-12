package br.com.ticpass.pos.feature.printing.state

import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrinterNetworkInfo
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType

/**
 * Represents an action that can be dispatched to the ViewModel
 * Actions trigger state transitions and side effects
 */
sealed class PrintingAction {
    // Queue actions
    object StartProcessing : PrintingAction()
    data class EnqueuePrinting(
        val filePath: String,
        val processorType: PrintingProcessorType,
    ) : PrintingAction()
    data class CancelPrinting(val printingId: String) : PrintingAction()
    object ClearQueue : PrintingAction()
    object AbortCurrentPrinting : PrintingAction()
    
    // Processor input actions
    data class ConfirmProcessor<T: QueueItem>(val requestId: String, val modifiedItem: T) : PrintingAction()

    data class SkipProcessor(val requestId: String) : PrintingAction()
    
    data class SkipProcessorOnError(val requestId: String) : PrintingAction()
    
    // Error handling actions
    data class HandleFailedPrinting(
        val requestId: String,
        val action: ErrorHandlingAction
    ) : PrintingAction()

    // Printer network info actions
    data class ConfirmPrinterNetworkInfo(
        val requestId: String,
        val networkInfo: PrinterNetworkInfo
    ) : PrintingAction()
    
    // Internal actions triggered by events
    data class ProcessingStateChanged(val state: ProcessingState<*>?) : PrintingAction()
    data class QueueInputRequested(val request: QueueInputRequest) : PrintingAction()
    data class ProcessorInputRequested(val request: UserInputRequest) : PrintingAction()
}
