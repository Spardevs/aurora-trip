package br.com.ticpass.pos.data.category.remote.service


import br.com.ticpass.pos.data.category.remote.dto.CategoriesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CategoryApiService {
    @GET("menu/categories/pos")
    suspend fun getCategories(
        @Query("menu") menuId: String
    ): CategoriesResponseDto
}