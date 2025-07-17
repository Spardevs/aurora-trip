package br.com.ticpass.pos.viewmodel.products

import android.util.Log
import androidx.lifecycle.*
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.data.room.entity.CategoryWithProducts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories
    private val _productsByCategory = MutableLiveData<Map<String, List<Product>>>()
    val productsByCategory: LiveData<Map<String, List<Product>>> = _productsByCategory

    fun loadCategoriesWithProducts() {

        viewModelScope.launch {
            val data: List<CategoryWithProducts> = repository.getCategoryWithProducts()
            val map = data.associate { catWith ->
                val uiList = catWith.products.map { entity ->
                    Product(
                        title = entity.name,
                        value = entity.price.toBigInteger(),
                        photo = entity.thumbnail,
                        fkCategory = catWith.category.name,
                        id = entity.id,
                        stock = entity.stock.toBigInteger(),
                        createdAt = "",
                        updatedAt = "",
                        deletedAt = "",
                        fkEvent = 0,
                    )
                }
                catWith.category.name to uiList
            }

            _categories.postValue(map.keys.toList())
            _productsByCategory.postValue(map)
        }
    }
}
