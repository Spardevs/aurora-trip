package br.com.ticpass.pos.core.queue.processors.payment.models

import br.com.ticpass.pos.core.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.payment.utils.SystemCustomerReceiptPrinting
import java.util.UUID

/**
 * Payment Queue Item
 * Represents a payment operation in the queue
 */
data class PaymentProcessingQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val customerReceiptPrinting: SystemCustomerReceiptPrinting = SystemCustomerReceiptPrinting.CONFIRMATION,
    val amount: Int,
    val commission: Int,
    val method: SystemPaymentMethod,
    var isTransactionless: Boolean = false,
) : QueueItem
