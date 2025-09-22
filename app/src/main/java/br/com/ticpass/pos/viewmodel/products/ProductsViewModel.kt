package br.com.ticpass.pos.viewmodel.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository,
    private val posRepository: PosRepository
) : ViewModel() {

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _productsByCategory = MutableLiveData<Map<String, List<Product>>>()
    val productsByCategory: LiveData<Map<String, List<Product>>> = _productsByCategory

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var allProductsList: List<Product> = emptyList()

    // Carrega categorias e produtos aplicando a comissão do POS selecionado
    fun loadCategoriesWithProducts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // obtém POS selecionado (suspend) — executa em IO
                val commissionPercent = withContext(Dispatchers.IO) {
                    try {
                        posRepository.getSelectedPos().commission
                    } catch (e: Exception) {
                        0L
                    }
                }

                val catsWithProds = categoryRepo.getCategoriesWithEnabledProducts()
                allProductsList = catsWithProds.flatMap { catWith ->
                    catWith.enabledProducts.map { entity ->
                        Product(
                            id = entity.id,
                            title = entity.name,
                            // aplica comissão aqui (assumindo commissionPercent em %)
                            value = applyCommission(entity.price, commissionPercent),
                            photo = entity.thumbnail,
                            stock = entity.stock.toLong(),
                            createdAt = "",
                            updatedAt = "",
                            deletedAt = "",
                            fkCategory = entity.categoryId,
                            fkEvent = 0
                        )
                    }
                }

                val allCategories = listOf("Todos") + catsWithProds.map { it.category.name }
                _categories.value = allCategories

                val productsMap = mutableMapOf<String, List<Product>>()
                productsMap["Todos"] = allProductsList
                catsWithProds.forEach { catWith ->
                    val categoryProducts = allProductsList.filter { it.fkCategory == catWith.category.id }
                    productsMap[catWith.category.name] = categoryProducts
                }
                _productsByCategory.value = productsMap

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Se commissionPercent for 10 (10%), calcula value + 10%
    private fun applyCommission(value: Long, commissionPercent: Long): Long {
        return value + (value * commissionPercent / 100)
    }
}