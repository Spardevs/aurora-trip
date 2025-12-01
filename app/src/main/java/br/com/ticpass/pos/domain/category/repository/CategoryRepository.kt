package br.com.ticpass.pos.domain.category.repository

import br.com.ticpass.pos.domain.category.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun refreshCategories(menuId: String)
}