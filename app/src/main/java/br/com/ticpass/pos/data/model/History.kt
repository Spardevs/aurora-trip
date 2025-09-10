package br.com.ticpass.pos.data.model

import br.com.ticpass.pos.data.room.entity.ProductEntity
import java.util.Date

data class History(
    val id: String,
    val transactionId: String,
    val totalPrice: Double,
    val paymentPrice: Double,
    val commissionPrice: Double,
    val paymentMethod: String,
    val date: Date,
    val description: String,
    val products: List<Pair<ProductEntity, Int>>
)