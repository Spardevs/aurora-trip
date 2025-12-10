package br.com.ticpass.pos.data.menupin.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing menu pins (whitelist of allowed pins for a menu)
 */
@Entity(tableName = "menu_pin")
data class MenuPinEntity(
    @PrimaryKey 
    val id: String,
    val code: String,
    val menuId: String,
    val userId: String,
    val userName: String?,
    val userEmail: String?,
    val userAvatar: Int?,
    val createdAt: String,
    val updatedAt: String
)
