package br.com.ticpass.pos.feature.payment.state

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.input.QueueInputRequest
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType

/**
 * Represents an action that can be dispatched to the ViewModel
 * Actions trigger state transitions and side effects
 */
sealed class PaymentProcessingAction {
    // Queue actions
    object StartProcessing : PaymentProcessingAction()
    data class EnqueuePayment(
        val amount: Int,
        val commission: Int = 0,
        val method: SystemPaymentMethod,
        val processorType: PaymentProcessorType
    ) : PaymentProcessingAction()
    data class CancelPayment(val paymentId: String) : PaymentProcessingAction()
    object CancelAllPayments : PaymentProcessingAction()
    
    // Processor input actions
    data class ConfirmProcessor<T: QueueItem>(val requestId: String, val modifiedItem: T) : PaymentProcessingAction()

    data class SkipProcessor(val requestId: String) : PaymentProcessingAction()
    
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
    data class UpdateAllProcessorTypes(val useTransactionless: Boolean) : PaymentProcessingAction()
}
