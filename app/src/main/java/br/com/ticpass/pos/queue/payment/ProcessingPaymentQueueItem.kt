package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.QueueItem
import br.com.ticpass.pos.queue.QueueItemStatus
import java.util.UUID

/**
 * Payment Queue Item
 * Represents a payment operation in the queue
 */
data class ProcessingPaymentQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val priority: Int = 0,
    override val status: QueueItemStatus = QueueItemStatus.PENDING,
    val amount: Double,
    val currency: String,
    val recipientId: String,
    val description: String,
    val processorType: String = "acquirer" // Defaults to acquirer processor
) : QueueItem
