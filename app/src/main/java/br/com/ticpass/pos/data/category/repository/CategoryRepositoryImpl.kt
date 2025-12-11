package br.com.ticpass.pos.data.category.repository

import br.com.ticpass.pos.data.category.datasource.CategoryLocalDataSource
import br.com.ticpass.pos.data.category.datasource.CategoryRemoteDataSource
import br.com.ticpass.pos.data.category.local.entity.CategoryEntity
import br.com.ticpass.pos.data.category.mapper.toDomain
import br.com.ticpass.pos.data.category.mapper.toEntity
import br.com.ticpass.pos.data.category.remote.dto.CategoriesResponseDto
import br.com.ticpass.pos.data.category.remote.dto.CategoryDto
import br.com.ticpass.pos.domain.category.model.Category
import br.com.ticpass.pos.domain.category.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val localDataSource: CategoryLocalDataSource,
    private val remoteDataSource: CategoryRemoteDataSource
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return localDataSource.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshCategories(menuId: String) {
        Timber.i("CategoryRepositoryImpl.refreshCategories menuId=%s", menuId)
        try {
            val response: CategoriesResponseDto = remoteDataSource.getCategories(menuId)
            val count = response.categories?.size ?: 0
            Timber.i("Received categories response: %d", count)

            val entities: List<CategoryEntity> = response.categories?.map { it.toEntity() } ?: emptyList()

            if (entities.isNotEmpty()) {
                localDataSource.insertAll(entities)
                Timber.i("Inserted %d categories into local database", entities.size)
            } else {
                Timber.i("No categories to insert")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing categories: ${e.message}")
            throw e
        }
    }
}