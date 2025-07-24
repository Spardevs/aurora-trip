package br.com.ticpass.pos.queue.payment.usecases

import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.payment.SystemPaymentMethod
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType
import br.com.ticpass.pos.queue.payment.state.SideEffect
import br.com.ticpass.pos.queue.payment.state.UiEvent
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for handling queue management operations
 */
class QueueManagementUseCase @Inject constructor() {
    
    /**
     * Start processing the payment queue
     */
    fun startProcessing(
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit
    ): SideEffect {
        emitUiEvent(UiEvent.ShowToast("Starting payment processing"))
        return SideEffect.StartProcessingQueue { paymentQueue.startProcessing() }
    }
    
    /**
     * Enqueue a new payment
     */
    fun enqueuePayment(
        amount: Int,
        commission: Int,
        method: SystemPaymentMethod,
        processorType: PaymentProcessorType,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit
    ): SideEffect {
        val paymentItem = ProcessingPaymentQueueItem(
            id = UUID.randomUUID().toString(),
            amount = amount,
            commission = commission,
            method = method,
            priority = 10,
            processorType = processorType
        )
        emitUiEvent(UiEvent.ShowToast("Payment added to queue"))
        return SideEffect.EnqueuePaymentItem { paymentQueue.enqueue(paymentItem) }
    }
    
    /**
     * Cancel a specific payment
     */
    fun cancelPayment(
        paymentId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit
    ): SideEffect {
        return SideEffect.RemovePaymentItem {
            val item = paymentQueue.queueState.value.find { it.id == paymentId }
            if (item != null) {
                paymentQueue.remove(item)
                emitUiEvent(UiEvent.ShowToast("Payment cancelled"))
            }
        }
    }
    
    /**
     * Cancel all payments
     */
    fun cancelAllPayments(
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit
    ): SideEffect {
        emitUiEvent(UiEvent.ShowToast("All payments cancelled"))
        return SideEffect.RemoveAllPaymentItems { paymentQueue.removeAll() }
    }
}
