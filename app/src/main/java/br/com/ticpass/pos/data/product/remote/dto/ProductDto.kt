package br.com.ticpass.pos.data.product.remote.dto

data class ProductDto(
    val id: String,
    val label: String,
    val price: Long,
    val thumbnail: ThumbnailDto,
    val category: CategoryDto?
)

data class ThumbnailDto(
    val id: String
)

data class CategoryDto(
    val id: String
)

data class ProductsResponseDto(
    val products: List<ProductDto>
)