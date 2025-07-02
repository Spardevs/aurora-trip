package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val coords: String,
    val synced: Boolean,
    val createdAt: String
)