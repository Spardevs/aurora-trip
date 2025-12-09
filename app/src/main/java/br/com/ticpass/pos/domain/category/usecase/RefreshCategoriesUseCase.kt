package br.com.ticpass.pos.domain.category.usecase

import br.com.ticpass.pos.domain.category.repository.CategoryRepository
import javax.inject.Inject

class RefreshCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(menuId: String) {
        repository.refreshCategories(menuId)
    }
}