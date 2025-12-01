package br.com.ticpass.pos.data.category.remote.service


import br.com.ticpass.pos.data.category.remote.dto.CategoriesResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CategoryApiService {
    @GET("menu/products/pos")
    suspend fun getCategories(
        @Query("menu") menuId: String,
        @Header("Content-Type") contentType: String = "application/json"
    ): CategoriesResponseDto
}