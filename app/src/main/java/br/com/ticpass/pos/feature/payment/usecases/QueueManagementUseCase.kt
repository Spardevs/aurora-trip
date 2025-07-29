package br.com.ticpass.pos.feature.payment.usecases

import br.com.ticpass.pos.feature.payment.state.UiEvent
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.processors.utils.PaymentMethodProcessorMapper
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType
import br.com.ticpass.pos.feature.payment.state.SideEffect
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
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
    
    /**
     * Update processor type for all queued items
     * Used when toggling transactionless mode
     */
    fun updateAllProcessorTypes(
        useTransactionless: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (UiEvent) -> Unit
    ): SideEffect {
        return SideEffect.UpdateAllProcessorTypes {
            val currentItems = paymentQueue.queueState.value
            val updatedItems = currentItems.map { item ->
                if (useTransactionless) {
                    item.copy(processorType = PaymentProcessorType.TRANSACTIONLESS)
                } else {
                    item.copy(processorType = PaymentMethodProcessorMapper.getProcessorTypeForMethod(item.method))
                }
            }
            
            // Remove all items and re-add the updated ones
            paymentQueue.removeAll()
            updatedItems.forEach { paymentQueue.enqueue(it) }
            
            val message = if (useTransactionless) {
                "All payments set to transactionless mode"
            } else {
                "Transactionless mode disabled"
            }
            emitUiEvent(UiEvent.ShowToast(message))
        }
    }
}
