package br.com.ticpass.pos.viewmodel.products

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _productsByCategory = MutableLiveData<Map<String, List<Product>>>()
    val productsByCategory: LiveData<Map<String, List<Product>>> = _productsByCategory

    fun loadCategoriesWithProducts() {
        viewModelScope.launch {
            val catsWithProds = categoryRepo.getCategoriesWithEnabledProducts()
            _categories.value = catsWithProds.map { it.category.name }
            _productsByCategory.value = catsWithProds.associate { catWith ->
                val listaUI = catWith.enabledProducts.map { entity ->
                    Product(
                        id = entity.id,
                        title = entity.name,
                        value = entity.price.toBigInteger(),
                        photo = entity.thumbnail,
                        stock = entity.stock.toBigInteger(),
                        createdAt = "",
                        updatedAt = "",
                        deletedAt = "",
                        fkCategory = entity.categoryId,
                        fkEvent = 0
                    )
                }
                catWith.category.name to listaUI
            }
        }
    }
}

