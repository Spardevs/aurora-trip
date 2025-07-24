package br.com.ticpass.pos.queue.payment.state

import br.com.ticpass.pos.queue.ErrorHandlingAction
import br.com.ticpass.pos.queue.ProcessingState
import br.com.ticpass.pos.queue.QueueInputRequest
import br.com.ticpass.pos.queue.payment.SystemPaymentMethod
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType

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
    data class ConfirmNextProcessor(val requestId: String) : Action()
    data class ConfirmNextProcessorWithModifiedPayment(
        val requestId: String,
        val modifiedAmount: Int,
        val modifiedMethod: SystemPaymentMethod,
        val modifiedProcessorType: PaymentProcessorType
    ) : Action()
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
    
    // Internal actions triggered by events
    data class ProcessingStateChanged(val state: ProcessingState<*>?) : Action()
    data class QueueInputRequested(val request: QueueInputRequest) : Action()
}
