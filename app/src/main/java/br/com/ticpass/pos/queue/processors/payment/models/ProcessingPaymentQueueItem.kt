package br.com.ticpass.pos.queue.processors.payment.models

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.payment.utils.SystemCustomerReceiptPrinting
import java.util.UUID

/**
 * Payment Queue Item
 * Represents a payment operation in the queue
 */
data class ProcessingPaymentQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val customerReceiptPrinting: SystemCustomerReceiptPrinting = SystemCustomerReceiptPrinting.CONFIRMATION,
    val amount: Int,
    val commission: Int,
    val method: SystemPaymentMethod,
    var isTransactionless: Boolean = false,
) : QueueItem
