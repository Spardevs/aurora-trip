package br.com.ticpass.pos.domain.product.repository

import br.com.ticpass.pos.domain.product.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getEnabledProducts(): Flow<List<Product>>
    suspend fun refreshProducts(menuId: String)
}