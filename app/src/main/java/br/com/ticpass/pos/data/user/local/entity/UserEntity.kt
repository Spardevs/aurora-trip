package br.com.ticpass.pos.data.user.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isLogged: Boolean = false
)