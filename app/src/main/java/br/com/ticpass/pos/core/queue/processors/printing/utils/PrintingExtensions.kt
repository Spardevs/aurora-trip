package br.com.ticpass.pos.core.queue.processors.printing.utils

import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEntity
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem

/**
 * Extension functions for converting between PrintingEntity and PrintingQueueItem
 */

/**
 * Convert PrintingQueueItem to PrintingEntity
 */
fun PrintingQueueItem.toEntity(): PrintingEntity {
    return PrintingEntity(
        id = id,
        priority = priority,
        status = status.toString(),
        filePath = filePath,
        processorType = processorType
    )
}

/**
 * Convert PrintingEntity to PrintingQueueItem
 */
fun PrintingEntity.toQueueItem(): PrintingQueueItem {
    return PrintingQueueItem(
        id = id,
        priority = priority,
        status = QueueItemStatus.valueOf(status.uppercase()),
        filePath = filePath,
        processorType = processorType
    )
}
