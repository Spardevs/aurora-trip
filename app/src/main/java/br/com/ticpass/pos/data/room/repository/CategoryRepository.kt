package br.com.ticpass.pos.data.room.repository

import javax.inject.Inject
import javax.inject.Singleton
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.entity.CategoryWithEnabledProducts
import br.com.ticpass.pos.data.room.entity.CategoryWithProducts

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    suspend fun getAll() = categoryDao.getAllCategories()

    suspend fun getCategoriesWithProducts(): List<CategoryWithProducts> =
        categoryDao.getCategoriesWithProducts()

    suspend fun getCategoriesWithEnabledProducts(): List<CategoryWithEnabledProducts> =
        categoryDao.getCategoriesWithEnabledProducts()

    suspend fun insertMany(categories: List<CategoryEntity>) {
        return categoryDao.insertMany(categories)
    }

    suspend fun getByIds(categoryIds: List<String>): List<CategoryEntity> {
        return categoryDao.getByIds(categoryIds)
    }

    suspend fun clearAll() {
        return categoryDao.clearAll()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: CategoryRepository? = null

        fun getInstance(categoryDao: CategoryDao) =
            instance ?: synchronized(this) {
                instance ?: CategoryRepository(categoryDao).also { instance = it }
            }
    }
}
