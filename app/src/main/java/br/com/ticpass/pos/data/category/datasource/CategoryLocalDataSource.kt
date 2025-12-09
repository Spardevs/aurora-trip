package br.com.ticpass.pos.data.category.datasource

import br.com.ticpass.pos.data.category.local.dao.CategoryDao
import br.com.ticpass.pos.data.category.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

class CategoryLocalDataSource @Inject constructor(private val categoryDao: CategoryDao) {
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertAll(categories: List<CategoryEntity>) {
        Timber.i("CategoryLocalDataSource.insertAll called with %d categories", categories.size)
        categoryDao.insertAll(categories)
    }

    suspend fun getCategoryById(id: String) = categoryDao.getCategoryById(id)
}