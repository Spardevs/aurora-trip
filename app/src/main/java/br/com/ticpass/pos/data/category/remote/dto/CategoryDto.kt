package br.com.ticpass.pos.data.category.remote.dto

import com.google.gson.annotations.SerializedName

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
data class CategoriesResponseDto(
    @SerializedName("categories")
    val categories: List<CategoryDto>? = emptyList()
)