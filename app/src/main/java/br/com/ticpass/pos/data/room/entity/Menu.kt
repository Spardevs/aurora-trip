package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String,
    val pin: String,
    val details: String,
    val dateStart: String,
    val dateEnd: String,
    val mode: String,
    val isSelected: Boolean
)