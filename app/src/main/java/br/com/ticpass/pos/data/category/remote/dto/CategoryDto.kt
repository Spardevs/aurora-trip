package br.com.ticpass.pos.data.category.remote.dto


data class CategoriesResponseDto(
    val categories: List<CategoryDto>?
)
data class CategoryDto(
    val id: String,
    val label: String,
    val locked: Boolean,
    val products: List<String>,
    val menu: String,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)
