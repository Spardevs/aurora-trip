package br.com.ticpass.pos.queue.processors.printing.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Print Entity
 * Room database entity for print queue items
 */
@Entity(tableName = "print_queue")
data class PrintEntity(
    @PrimaryKey
    val id: String,
    val priority: Int,
    val content: String,
    val copies: Int,
    val paperSize: String, // Store as string, will be converted back to enum
    val printerId: String?,
    val status: String, // "pending", "processing", "completed", "failed"
    val metadata: String // Store as JSON string, will be converted
)
