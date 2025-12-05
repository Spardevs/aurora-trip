package br.com.ticpass.pos.data.category.repository

import br.com.ticpass.pos.data.category.datasource.CategoryLocalDataSource
import br.com.ticpass.pos.data.category.datasource.CategoryRemoteDataSource
import br.com.ticpass.pos.data.category.mapper.toDomain
import br.com.ticpass.pos.data.category.mapper.toEntity
import br.com.ticpass.pos.domain.category.model.Category
import br.com.ticpass.pos.domain.category.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        try {
            val response = remoteDataSource.getCategories(menuId)
            val entities = response.categories?.map { it.toDomain().toEntity() } ?: emptyList()
            localDataSource.insertAll(entities)
        } catch (e: Exception) {
            // Log error or handle accordingly
            throw e
        }
    }
}