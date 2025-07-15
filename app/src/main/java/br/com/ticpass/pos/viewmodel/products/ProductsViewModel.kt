package br.com.ticpass.pos.viewmodel.products

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val apiRepository: APIRepository,
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    private val _allProducts      = MutableLiveData<List<Product>>()
    private val _filteredProducts = MutableLiveData<List<Product>>()
    private val _categories       = MutableLiveData<List<String>>()
    private val _errorMessage     = MutableLiveData<String>()

    val filteredProducts: LiveData<List<Product>> = _filteredProducts
    val categories:       LiveData<List<String>> = _categories
    val errorMessage:     LiveData<String>       = _errorMessage

    /** Chama a API e popula _allProducts e _categories */
    fun fetchProducts() {
        viewModelScope.launch {
            val session = ctx.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            val user    = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val menuId  = session.getString("selected_menu_id", null)
            val jwt     = user.getString("auth_token", null)

            if (menuId.isNullOrBlank() || jwt.isNullOrBlank()) {
                _errorMessage.value = "Credenciais inválidas"
                return@launch
            }

            try {
                val resp = apiRepository.getEventProducts(event = menuId, jwt = jwt)
                if (resp.status == 200) {
                    // mapeia do DTO da API para o nosso UI model
                    val mapped = resp.result.flatMap { cat ->
                        cat.products.map { p ->
                            Product(
                                title = p.title,
                                photo = p.photo,
                                value = p.value,
                                fkCategory = cat.name,
                                id = p.id,
                                stock = p.stock,
                                createdAt = p.createdAt,
                                updatedAt = p.updatedAt,
                                deletedAt = p.deletedAt,
                                fkEvent = p.fkEvent,
                            )
                        }
                    }
                    _allProducts.value = mapped
                    _filteredProducts.value = mapped
                    _categories.value = listOf("Todos") + resp.result.map { it.name }
                } else {
                    _errorMessage.value = "Erro ${resp.status}: ${resp.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Falha na requisição"
            }
        }
    }

    /** Filtra por categoria em memória */
    fun filterByCategory(category: String) {
        val all = _allProducts.value.orEmpty()
        _filteredProducts.value = if (category == "Todos") all
        else all.filter { it.fkCategory == category }
    }
}
