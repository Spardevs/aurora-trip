package br.com.ticpass.pos.domain.category.usecase

import br.com.ticpass.pos.domain.category.model.Category
import br.com.ticpass.pos.domain.category.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(private val repository: CategoryRepository) {
    operator fun invoke(): Flow<List<Category>> = repository.getAllCategories()
}