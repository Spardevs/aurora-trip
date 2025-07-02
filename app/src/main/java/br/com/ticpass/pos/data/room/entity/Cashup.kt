package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cashup")
data class CashupEntity(
    @PrimaryKey val id: String,
    val accountable: String,
    val createdAt: Date,
    val initial: Long,
    val taken: Long,
    val remaining: Long,
    val synced: Boolean
)