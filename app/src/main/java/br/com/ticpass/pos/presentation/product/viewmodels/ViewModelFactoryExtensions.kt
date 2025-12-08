package br.com.ticpass.pos.presentation.product.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

fun <T : ViewModel> ViewModelProvider.Factory.create(
    modelClass: Class<T>,
    categoryId: String?
): T {
    if (this is ProductViewModel.Factory) {
        @Suppress("UNCHECKED_CAST")
        return this.create(categoryId) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
}