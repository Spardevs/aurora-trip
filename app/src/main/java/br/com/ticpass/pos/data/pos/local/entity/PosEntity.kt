package br.com.ticpass.pos.data.pos.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos")
data class PosEntity(
    @PrimaryKey val id: String,
    val prefix: String,
    val sequence: Int,
    val mode: String,
    @ColumnInfo(defaultValue = "0")
    val commission: Long = 0L,
    val menu: String,
    val sessionId: String? = null,
    val cashierId: String? = null,
    val cashierName: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val isClosed: Boolean = false,
    val isSelected: Boolean = false
)