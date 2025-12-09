package br.com.ticpass.pos.presentation.product.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.product.model.ProductModel
import br.com.ticpass.pos.domain.product.usecase.GetProductsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel @AssistedInject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    @Assisted private val categoryId: String?
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductModel>>(emptyList())
    val products: StateFlow<List<ProductModel>> = _products

    init {
        if (categoryId == "all" || categoryId == null) {
            loadProducts()
        } else {
            loadProductsByCategory(categoryId)
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            getProductsUseCase.invoke().collect { productsList ->
                _products.value = productsList
            }
        }
    }

    private fun loadProductsByCategory(categoryId: String) {
        viewModelScope.launch {
            getProductsUseCase.invoke().collect { productsList ->
                _products.value = productsList.filter { it.category == categoryId }
            }
        }
    }

    fun refreshProducts() {
        // Limpar os produtos atuais
        _products.value = emptyList()

        // Recarregar os produtos
        if (categoryId == "all" || categoryId == null) {
            loadProducts()
        } else {
            loadProductsByCategory(categoryId)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(categoryId: String?): ProductViewModel
    }
}