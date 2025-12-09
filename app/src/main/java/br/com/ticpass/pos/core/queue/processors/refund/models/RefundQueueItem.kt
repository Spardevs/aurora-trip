package br.com.ticpass.pos.core.queue.processors.refund.models

import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.RefundProcessorType
import java.util.UUID

/**
 * Refund Queue Item
 * Represents a refund operation in the queue
 */
data class RefundQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val processorType: RefundProcessorType,
    val atk: String,
    val txId: String,
    val isQRCode: Boolean = false,
) : QueueItem
