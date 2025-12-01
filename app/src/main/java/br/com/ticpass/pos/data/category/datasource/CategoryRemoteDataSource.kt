package br.com.ticpass.pos.data.category.datasource

import br.com.ticpass.pos.data.category.remote.dto.CategoriesResponseDto
import br.com.ticpass.pos.data.category.remote.service.CategoryApiService
import javax.inject.Inject

class CategoryRemoteDataSource @Inject constructor(private val apiService: CategoryApiService) {
    suspend fun getCategories(menuId: String): CategoriesResponseDto {
        return apiService.getCategories(menuId)
    }
}