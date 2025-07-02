package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pass",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = CASCADE),
        ForeignKey(entity = MenuEntity::class,  parentColumns = ["id"], childColumns = ["menuId"],  onDelete = CASCADE),
        ForeignKey(entity = PosEntity::class,   parentColumns = ["id"], childColumns = ["posId"],   onDelete = CASCADE)
    ],
    indices = [Index("orderId"), Index("menuId"), Index("posId")]
)
data class PassEntity(
    @PrimaryKey val id: String,
    val createdAt: String,
    val accountable: String,
    val orderId: String,
    val menuId: String,
    val posId: String,
    val mode: Boolean,
    val printingRetries: Int,
    val synced: Boolean
)