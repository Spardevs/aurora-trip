package br.com.ticpass.pos.queue.payment

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Payment Entity
 * Room database entity for storing payment queue items
 */
@Entity(tableName = "payment_queue")
data class ProcessingPaymentEntity(
    @PrimaryKey
    val id: String,
    val priority: Int,
    val status: String,
    val amount: Int,
    val commission: Int,
    val method: String,
)
