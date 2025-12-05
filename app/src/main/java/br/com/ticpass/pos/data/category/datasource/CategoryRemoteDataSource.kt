package br.com.ticpass.pos.data.category.datasource

import br.com.ticpass.pos.data.category.remote.dto.CategoriesResponseDto
import br.com.ticpass.pos.data.category.remote.service.CategoryApiService
import timber.log.Timber
import javax.inject.Inject

class CategoryRemoteDataSource @Inject constructor(private val apiService: CategoryApiService) {
    suspend fun getCategories(menuId: String): CategoriesResponseDto {
        val response = apiService.getCategories(menuId)
        Timber.tag("Categorias").i("Calling getCategories with menuId: $menuId")
        Timber.tag("Categorias").i("Received response from getCategories: $response")
        return response
    }
}