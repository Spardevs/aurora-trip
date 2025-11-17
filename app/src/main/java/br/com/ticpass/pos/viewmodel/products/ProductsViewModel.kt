package br.com.ticpass.pos.viewmodel.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.data.api.ProductThumbnail
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.EventRepository
import br.com.ticpass.pos.data.room.repository.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository,
    private val posRepository: PosRepository,
    private val eventRepository: EventRepository
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
                val commissionPercent = withContext(Dispatchers.IO) {
                    try {
                        posRepository.getSelectedPos().commission
                    } catch (e: Exception) {
                        0L
                    }
                }

                val selectedEventId = withContext(Dispatchers.IO) {
                    try {
                        eventRepository.getSelectedEvent()?.id ?: "0"
                    } catch (e: Exception) {
                        "0"
                    }
                }

                val selectedEventIdInt = selectedEventId.toIntOrNull() ?: 0
                val catsWithProds = categoryRepo.getCategoriesWithEnabledProducts()
                allProductsList = catsWithProds.flatMap { catWith ->
                    catWith.enabledProducts.map { entity ->
                        // Mapear ProductEntity -> API Product (preencher campos obrigatórios)
                        Product(
                            id = entity.id,
                            label = entity.name,
                            price = applyCommission(entity.price, commissionPercent),
                            thumbnail = ProductThumbnail(
                                id = entity.thumbnail,
                                transparency = 0,
                                size = 0,
                                width = 0,
                                height = 0,
                                ext = "",
                                mimetype = "",
                                createdBy = "",
                                createdAt = "",
                                updatedAt = ""
                            ),
                            category = entity.categoryId,
                            menu = "",
                            createdBy = "",
                            createdAt = "",
                            updatedAt = ""
                        )
                    }
                }

                val allCategories = listOf("Todos") + catsWithProds.map { it.category.name }
                _categories.value = allCategories

                val productsMap = mutableMapOf<String, List<Product>>()
                productsMap["Todos"] = allProductsList
                catsWithProds.forEach { catWith ->
                    val categoryProducts = allProductsList.filter { it.category == catWith.category.id }
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

    // Se commissionPercent for 10 (10%), calcula value + 10% e retorna Int (compatível com Product.price)
    private fun applyCommission(value: Long, commissionPercent: Long): Int {
        val result = value + (value * commissionPercent / 100)
        return result.toInt()
    }
}