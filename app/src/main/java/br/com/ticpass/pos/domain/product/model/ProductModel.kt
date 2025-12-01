package br.com.ticpass.pos.domain.product.model

data class Product(
    val id: String,
    val category: String,
    val name: String,
    val thumbnail: String,
    val price: Long,
    val stock: Int,
    val isEnabled: Boolean
)