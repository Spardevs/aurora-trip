package br.com.ticpass.pos.presentation.payment.states

import br.com.ticpass.pos.core.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.core.queue.input.UserInputRequest
import br.com.ticpass.pos.core.queue.models.ProcessingState
import br.com.ticpass.pos.core.queue.input.QueueInputRequest

/**
 * Represents an action that can be dispatched to the ViewModel
 * Actions trigger state transitions and side effects
 */
open class PaymentProcessingAction {
    // Queue actions
    object StartProcessing : PaymentProcessingAction()
    data class EnqueuePayment(
        val amount: Int,
        val commission: Int = 0,
        val method: SystemPaymentMethod,
        val isTransactionless: Boolean
    ) : PaymentProcessingAction()
    data class CancelPayment(val paymentId: String) : PaymentProcessingAction()
    object ClearQueue : PaymentProcessingAction()
    object AbortCurrentPayment : PaymentProcessingAction()
    
    // Processor input actions
    data class ConfirmProcessor<T: QueueItem>(val requestId: String, val modifiedItem: T) : PaymentProcessingAction()

    data class SkipProcessor(val requestId: String) : PaymentProcessingAction()
    
    data class SkipProcessorOnError(val requestId: String) : PaymentProcessingAction()
    
    // Error handling actions
    data class HandleFailedPayment(
        val requestId: String,
        val action: ErrorHandlingAction
    ) : PaymentProcessingAction()
    
    // Receipt printing actions
    data class ConfirmCustomerReceiptPrinting(
        val requestId: String,
        val shouldPrint: Boolean
    ) : PaymentProcessingAction()
    
    // PIX payment actions
    data class ConfirmMerchantPixKey(
        val requestId: String,
        val pixKey: String
    ) : PaymentProcessingAction()
    
    data class ConfirmMerchantPixHasBeenPaid(
        val requestId: String,
        val didPay: Boolean
    ) : PaymentProcessingAction()
    
    // Internal actions triggered by events
    data class ProcessingStateChanged(val state: ProcessingState<*>?) : PaymentProcessingAction()
    data class QueueInputRequested(val request: QueueInputRequest) : PaymentProcessingAction()
    data class ProcessorInputRequested(val request: UserInputRequest) : PaymentProcessingAction()
    
    // Transactionless mode actions
    data class ToggleTransactionless(val useTransactionless: Boolean) : PaymentProcessingAction()
}
