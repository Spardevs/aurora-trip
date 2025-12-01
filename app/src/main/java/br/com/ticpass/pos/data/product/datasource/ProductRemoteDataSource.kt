package br.com.ticpass.pos.data.product.datasource

import br.com.ticpass.pos.data.product.remote.dto.ProductsResponseDto
import br.com.ticpass.pos.data.product.remote.service.ProductApiService
import okhttp3.ResponseBody
import javax.inject.Inject

class ProductRemoteDataSource @Inject constructor(private val apiService: ProductApiService) {
    suspend fun getProducts(menuId: String): ProductsResponseDto {
        return apiService.getProducts(menuId)
    }

    suspend fun downloadThumbnails(menuId: String): ResponseBody {
        return apiService.downloadThumbnails(menuId)
    }
}