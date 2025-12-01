package br.com.ticpass.pos.data.product.remote.service

import br.com.ticpass.pos.data.product.remote.dto.ProductsResponseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ProductApiService {
    @GET("menu/products/pos")
    suspend fun getProducts(
        @Query("menu") menuId: String,
        @Header("Content-Type") contentType: String = "application/json",
    ): ProductsResponseDto

    @GET("menu/{menuId}/product/thumbnail/download/all")
    @Streaming
    suspend fun downloadThumbnails(
        @Path("menuId") menuId: String,
        @Header("Content-Type") contentType: String = "application/json"
    ): ResponseBody
}