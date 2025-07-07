package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.CategoryWithProducts
import br.com.ticpass.pos.data.room.entity.ProductEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    suspend fun getCategoryWithProducts(): List<CategoryWithProducts> {

        val categoryWithProducts = productDao.getCategoryWithProducts()

        val populated = categoryWithProducts.map { cat ->
            val newProds = cat.products.map{
                it.copy(category = cat.category.name)
            }

            cat.copy(products = newProds)
        }


        return populated
    }

    suspend fun getAllProducts() = productDao.getAll()

    suspend fun insertMany(events: List<ProductEntity>) {
        return productDao.insertMany(events)
    }

    suspend fun toggleEnable(productId: String) {

        val eventToUpdate = productDao.getById(productId)

        if (eventToUpdate != null) {
            eventToUpdate.isEnabled = !eventToUpdate.isEnabled
            productDao.updateProduct(eventToUpdate)
        }
    }

    suspend fun clearAll() {
        return productDao.clearAll()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: ProductRepository? = null

        fun getInstance(eventDao: ProductDao) =
            instance ?: synchronized(this) {
                instance ?: ProductRepository(eventDao).also { instance = it }
            }
    }
}
