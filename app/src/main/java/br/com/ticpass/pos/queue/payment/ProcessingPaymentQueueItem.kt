package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.QueueItem
import br.com.ticpass.pos.queue.QueueItemStatus
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType
import java.util.UUID

/**
 * Payment Queue Item
 * Represents a payment operation in the queue
 */
data class ProcessingPaymentQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override val status: QueueItemStatus = QueueItemStatus.PENDING,
    val customerReceiptPrinting: SystemCustomerReceiptPrinting = SystemCustomerReceiptPrinting.CONFIRMATION,
    val processorType: PaymentProcessorType = PaymentProcessorType.ACQUIRER,
    val amount: Int,
    val commission: Int,
    val method: SystemPaymentMethod,
) : QueueItem
