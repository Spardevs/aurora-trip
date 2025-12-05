package br.com.ticpass.pos.queue.processors.printing.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType

/**
 * Printing Entity
 * Room database entity for storing printing queue items
 */
@Entity(tableName = "printing_queue")
data class PrintingEntity(
    @PrimaryKey
    val id: String,
    val priority: Int,
    val status: String,
    val filePath: String,
    val processorType: PrintingProcessorType,
)
