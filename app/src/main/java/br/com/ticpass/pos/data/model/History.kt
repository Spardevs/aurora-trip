package br.com.ticpass.pos.data.model

import br.com.ticpass.pos.data.room.entity.ProductEntity
import java.util.Date
import java.util.UUID

data class History(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val totalPrice: Double,
    val paymentPrice: Double,
    val commissionPrice: Double,
    val date: Date,
    val paymentMethod: String,
    val description: String,
    val products: List<Pair<ProductEntity, Int>> = emptyList() // ProductEntity + quantidade
)