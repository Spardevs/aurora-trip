package br.com.ticpass.pos.data.room.entity
import androidx.room.Entity

import androidx.room.PrimaryKey
@Entity(tableName = "pos")
data class PosEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cashier: String,
    val commission: Long,
    val closedAt: Boolean,
    val isSelected: Boolean
)