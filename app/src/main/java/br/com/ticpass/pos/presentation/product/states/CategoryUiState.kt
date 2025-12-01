package br.com.ticpass.pos.presentation.product.states

import br.com.ticpass.pos.domain.category.model.Category

sealed class CategoryUiState {
    object Loading : CategoryUiState()
    data class Success(val categories: List<Category>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}