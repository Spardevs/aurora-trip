package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val atk: String,
    val amount: Long,
    val commission: Long,
    val createdAt: String,
    val method: String,
    val usedAcquirer: Boolean,
    val synced: Boolean
)