package br.com.ticpass.pos.data.menu.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val logo: String?, // logo path
    val pin: String,
    val details: String,
    val dateStart: String,
    val dateEnd: String,
    val mode: String
)