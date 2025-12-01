package br.com.ticpass.pos.presentation.product.states

import br.com.ticpass.pos.domain.product.model.Product

sealed class ProductUiState {
    object Loading : ProductUiState()
    data class Success(val products: List<Product>) : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}