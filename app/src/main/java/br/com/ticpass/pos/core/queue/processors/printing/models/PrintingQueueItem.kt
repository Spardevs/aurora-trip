package br.com.ticpass.pos.core.queue.processors.printing.models

import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.printing.processors.models.PrintingProcessorType
import java.util.UUID

/**
 * Printing Queue Item
 * Represents a printing operation in the queue
 */
data class PrintingQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val filePath: String,
    val processorType: PrintingProcessorType,
) : QueueItem
