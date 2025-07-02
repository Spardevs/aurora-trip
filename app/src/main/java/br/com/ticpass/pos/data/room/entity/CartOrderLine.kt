package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity

@Entity(
    tableName = "cart_order_line",
    primaryKeys = ["product"]
)
data class CartOrderLineEntity(
    val product: String,
    val count: Int
)