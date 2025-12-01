package br.com.ticpass.pos.domain.product.usecase

import br.com.ticpass.pos.domain.category.repository.CategoryRepository

class RefreshCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(menuId: String) {
        categoryRepository.refreshCategories(menuId)
    }
}