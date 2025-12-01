package br.com.ticpass.pos.domain.product.usecase


import br.com.ticpass.pos.domain.product.model.Product
import br.com.ticpass.pos.domain.product.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(): Flow<List<Product>> = repository.getEnabledProducts()
}