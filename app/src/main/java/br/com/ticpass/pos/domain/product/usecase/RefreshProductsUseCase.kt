package br.com.ticpass.pos.domain.product.usecase

import br.com.ticpass.pos.domain.product.repository.ProductRepository
import javax.inject.Inject

class RefreshProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(menuId: String) {
        productRepository.refreshProducts(menuId)
    }
}