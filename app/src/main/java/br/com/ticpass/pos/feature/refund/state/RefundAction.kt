package br.com.ticpass.pos.feature.refund.state

import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.refund.processors.models.PrinterNetworkInfo
import br.com.ticpass.pos.queue.processors.refund.processors.models.RefundProcessorType

/**
 * Represents an action that can be dispatched to the ViewModel
 * Actions trigger state transitions and side effects
 */
sealed class RefundAction {
    // Queue actions
    object StartProcessing : RefundAction()
    data class EnqueueRefund(
        val atk: String,
        val txId: String,
        val isQRCode: Boolean,
        val processorType: RefundProcessorType,
    ) : RefundAction()
    data class CancelRefund(val refundId: String) : RefundAction()
    object ClearQueue : RefundAction()
    object AbortCurrentRefund : RefundAction()
    
    // Processor input actions
    data class ConfirmProcessor<T: QueueItem>(val requestId: String, val modifiedItem: T) : RefundAction()

    data class SkipProcessor(val requestId: String) : RefundAction()
    
    data class SkipProcessorOnError(val requestId: String) : RefundAction()
    
    // Error handling actions
    data class HandleFailedRefund(
        val requestId: String,
        val action: ErrorHandlingAction
    ) : RefundAction()

    // Printer network info actions
    data class ConfirmPrinterNetworkInfo(
        val requestId: String,
        val networkInfo: PrinterNetworkInfo
    ) : RefundAction()
    
    // Internal actions triggered by events
    data class ProcessingStateChanged(val state: ProcessingState<*>?) : RefundAction()
    data class QueueInputRequested(val request: QueueInputRequest) : RefundAction()
    data class ProcessorInputRequested(val request: UserInputRequest) : RefundAction()
}
