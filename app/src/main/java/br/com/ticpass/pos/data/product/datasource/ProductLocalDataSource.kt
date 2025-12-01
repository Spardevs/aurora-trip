package br.com.ticpass.pos.data.product.datasource

import br.com.ticpass.pos.data.product.local.dao.ProductDao
import br.com.ticpass.pos.data.product.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProductLocalDataSource @Inject constructor(private val productDao: ProductDao) {
    fun getEnabledProducts(): Flow<List<ProductEntity>> = productDao.getEnabledProducts()

    suspend fun insertAll(products: List<ProductEntity>) = productDao.insertAll(products)
}