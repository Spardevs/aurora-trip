package br.com.ticpass.pos.presentation.product.states

import br.com.ticpass.pos.domain.product.model.ProductModel

sealed class ProductUiState {
    object Loading : ProductUiState()
    data class Success(val products: List<ProductModel>) : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}