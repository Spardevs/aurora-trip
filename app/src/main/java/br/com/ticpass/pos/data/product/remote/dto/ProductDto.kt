package br.com.ticpass.pos.data.product.remote.dto

data class ProductDto(
    val id: String,
    val label: String,
    val price: Int,
    val thumbnail: ThumbnailDto?,
    val category: String?,  // corrigido para String?
    val menu: String,
    val createdBy: String,  // já está correto como String
    val createdAt: String,
    val updatedAt: String
)

data class ThumbnailDto(
    val id: String,
    val transparency: Int,
    val size: Int,
    val width: Int,
    val height: Int,
    val ext: String,
    val mimetype: String,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)
data class ProductsResponseDto(
    val products: List<ProductDto>
)