package br.com.ticpass.pos.presentation.product.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject

class ProductViewModelFactory @Inject constructor(
    private val assistedFactory: ProductViewModel.Factory,
    private val categoryId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return assistedFactory.create(categoryId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}