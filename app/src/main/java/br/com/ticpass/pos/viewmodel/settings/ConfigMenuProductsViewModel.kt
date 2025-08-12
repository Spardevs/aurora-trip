package br.com.ticpass.pos.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.data.room.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigMenuProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    private val _loadingItemId = MutableStateFlow<Long?>(null)

    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()
    val loadingItemId: StateFlow<Long?> = _loadingItemId.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _products.value = productRepository.getAllProducts()
        }
    }

    fun toggleProductStatus(product: ProductEntity) {
        viewModelScope.launch {
            _loadingItemId.value = product.id.toLong()
            try {
                productRepository.toggleEnable(product.id)
                _products.value = _products.value.map {
                    if (it.id == product.id) it.copy(isEnabled = !it.isEnabled) else it
                }
            } finally {
                _loadingItemId.value = null
            }
        }
    }
}