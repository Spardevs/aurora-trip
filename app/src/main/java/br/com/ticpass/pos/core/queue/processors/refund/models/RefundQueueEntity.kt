package br.com.ticpass.pos.core.queue.processors.refund.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.RefundProcessorType

/**
 * Refund Queue Entity
 * Room database entity for storing refund queue items
 */
@Entity(tableName = "refund_queue")
data class RefundQueueEntity(
    @PrimaryKey
    val id: String,
    val priority: Int,
    val status: String,
    val processorType: RefundProcessorType,
    val atk: String,
    val txId: String,
)
