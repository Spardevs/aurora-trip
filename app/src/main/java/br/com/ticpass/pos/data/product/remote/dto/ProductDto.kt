package br.com.ticpass.pos.data.product.remote.dto

import br.com.ticpass.pos.data.auth.remote.dto.UserDto
import br.com.ticpass.pos.data.category.remote.dto.CategoryDto
import br.com.ticpass.pos.data.menu.remote.dto.MenuEdge

data class ProductDto(
    val id: String,
    val label: String,
    val price: Int,
    val thumbnail: ThumbnailDto,
    val category: String,  // objeto, não string
    val menu: String,
    val createdBy: String,     // objeto, não string
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