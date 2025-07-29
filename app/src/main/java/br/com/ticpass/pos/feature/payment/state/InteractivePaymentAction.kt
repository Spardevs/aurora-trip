package br.com.ticpass.pos.feature.payment.state

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.input.InputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType

/**
 * Represents an action that can be dispatched to the ViewModel
 * Actions trigger state transitions and side effects
 */
sealed class Action {
    // Queue actions
    object StartProcessing : Action()
    data class EnqueuePayment(
        val amount: Int,
        val commission: Int = 0,
        val method: SystemPaymentMethod,
        val processorType: PaymentProcessorType
    ) : Action()
    data class CancelPayment(val paymentId: String) : Action()
    object CancelAllPayments : Action()
    
    // Processor input actions
    data class ConfirmProcessor<T: QueueItem>(val requestId: String, val modifiedItem: T) : Action()

    data class SkipProcessor(val requestId: String) : Action()
    
    // Error handling actions
    data class HandleFailedPayment(
        val requestId: String,
        val action: ErrorHandlingAction
    ) : Action()
    
    // Receipt printing actions
    data class ConfirmCustomerReceiptPrinting(
        val requestId: String,
        val shouldPrint: Boolean
    ) : Action()
    
    // PIX payment actions
    data class ConfirmMerchantPixKey(
        val requestId: String,
        val pixKey: String
    ) : Action()
    
    data class ConfirmMerchantPixHasBeenPaid(
        val requestId: String,
        val didPay: Boolean
    ) : Action()
    
    // Internal actions triggered by events
    data class ProcessingStateChanged(val state: ProcessingState<*>?) : Action()
    data class QueueInputRequested(val request: QueueInputRequest) : Action()
    data class ProcessorInputRequested(val request: InputRequest) : Action()
    
    // Transactionless mode actions
    data class UpdateAllProcessorTypes(val useTransactionless: Boolean) : Action()
}
