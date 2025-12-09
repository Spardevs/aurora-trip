package br.com.ticpass.pos.domain.product.model

data class ProductModel(
    val id: String,
    val category: String,
    val name: String,
    val thumbnail: String,
    val price: Long,
    val stock: Int,
    val isEnabled: Boolean
)