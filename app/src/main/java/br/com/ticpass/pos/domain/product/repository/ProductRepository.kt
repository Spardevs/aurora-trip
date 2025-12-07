package br.com.ticpass.pos.domain.product.repository

import br.com.ticpass.pos.domain.product.model.ProductModel
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ProductRepository {
    fun getEnabledProducts(): Flow<List<ProductModel>>
    suspend fun refreshProducts(menuId: String)
    suspend fun downloadAndExtractThumbnails(menuId: String, thumbnailsDir: File)
}