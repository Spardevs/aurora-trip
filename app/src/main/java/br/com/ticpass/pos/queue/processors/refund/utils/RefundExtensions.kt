package br.com.ticpass.pos.queue.processors.refund.utils

import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueEntity
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueItem

/**
 * Extension functions for converting between RefundEntity and RefundQueueItem
 */

/**
 * Convert RefundQueueItem to RefundEntity
 */
fun RefundQueueItem.toEntity(): RefundQueueEntity {
    return RefundQueueEntity(
        id = id,
        priority = priority,
        status = status.toString(),
        atk = atk,
        txId = txId,
        processorType = processorType
    )
}

/**
 * Convert RefundEntity to RefundQueueItem
 */
fun RefundQueueEntity.toQueueItem(): RefundQueueItem {
    return RefundQueueItem(
        id = id,
        priority = priority,
        status = QueueItemStatus.valueOf(status.uppercase()),
        processorType = processorType,
        atk = atk,
        txId = txId,
    )
}
