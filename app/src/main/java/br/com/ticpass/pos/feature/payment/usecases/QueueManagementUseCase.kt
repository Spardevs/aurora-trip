package br.com.ticpass.pos.feature.payment.usecases

import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.processors.utils.PaymentMethodProcessorMapper
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingSideEffect
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
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    ): PaymentProcessingSideEffect {
        emitUiEvent(PaymentProcessingUiEvent.ShowToast("Starting payment processing"))
        return PaymentProcessingSideEffect.StartProcessingQueue { paymentQueue.startProcessing() }
    }
    
    /**
     * Enqueue a new payment
     */
    fun enqueuePayment(
        amount: Int,
        commission: Int,
        method: SystemPaymentMethod,
        isTransactionless: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    ): PaymentProcessingSideEffect {
        val paymentItem = ProcessingPaymentQueueItem(
            id = UUID.randomUUID().toString(),
            amount = amount,
            commission = commission,
            method = method,
            isTransactionless = isTransactionless,
            priority = 10,
        )
        emitUiEvent(PaymentProcessingUiEvent.ShowToast("Payment added to queue"))
        return PaymentProcessingSideEffect.EnqueuePaymentItem { paymentQueue.enqueue(paymentItem) }
    }
    
    /**
     * Cancel a specific payment
     */
    fun cancelPayment(
        paymentId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    ): PaymentProcessingSideEffect {
        return PaymentProcessingSideEffect.RemovePaymentItem {
            val item = paymentQueue.queueState.value.find { it.id == paymentId }
            if (item != null) {
                paymentQueue.remove(item)
                emitUiEvent(PaymentProcessingUiEvent.ShowToast("Payment cancelled"))
            }
        }
    }
    
    /**
     * Cancel all payments
     */
    fun cancelAllPayments(
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    ): PaymentProcessingSideEffect {
        emitUiEvent(PaymentProcessingUiEvent.ShowToast("All payments cancelled"))
        return PaymentProcessingSideEffect.RemoveAllPaymentItems { paymentQueue.removeAll() }
    }

    fun abortCurrentPayment(
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    ): PaymentProcessingSideEffect {
        emitUiEvent(PaymentProcessingUiEvent.ShowToast("Aborting current payment"))
        return PaymentProcessingSideEffect.AbortCurrentPayment { paymentQueue.abortCurrentPayment() }
    }
    
    /**
     * Update processor type for all queued items
     * Used when toggling transactionless mode
     */
    fun toggleTransactionless(
        useTransactionless: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        emitUiEvent: (PaymentProcessingUiEvent) -> Unit
    ): PaymentProcessingSideEffect {
        return PaymentProcessingSideEffect.UpdateAllProcessorTypes {
            val currentItems = paymentQueue.queueState.value
            val updatedItems = currentItems.map { item ->
                item.copy(isTransactionless = useTransactionless)
            }
            
            // Remove all items and re-add the updated ones
            paymentQueue.removeAll()
            updatedItems.forEach { paymentQueue.enqueue(it) }
            
            val message = if (useTransactionless) {
                "All payments set to transactionless mode"
            } else {
                "Transactionless mode disabled"
            }
            emitUiEvent(PaymentProcessingUiEvent.ShowToast(message))
        }
    }
}
