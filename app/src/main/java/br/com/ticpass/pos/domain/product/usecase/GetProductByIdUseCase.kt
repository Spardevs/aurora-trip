package br.com.ticpass.pos.domain.product.usecase

import br.com.ticpass.pos.domain.product.model.ProductModel
import br.com.ticpass.pos.domain.product.repository.ProductRepository
import javax.inject.Inject

class GetProductByIdUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(id: String): ProductModel? =
        repository.getProductById(id)
}