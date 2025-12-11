package br.com.ticpass.pos.presentation.shoppingCart.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.core.util.ShoppingCartUtils
import br.com.ticpass.pos.domain.product.model.ProductModel
import br.com.ticpass.pos.domain.product.usecase.GetProductByIdUseCase
import br.com.ticpass.pos.domain.shoppingCart.model.CartItemModel
import br.com.ticpass.pos.presentation.shoppingCart.states.CartUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    application: Application,
    private val getProductByIdUseCase: GetProductByIdUseCase
) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext

    private val _cartItems = MutableStateFlow<List<CartItemModel>>(emptyList())
    val cartItems: StateFlow<List<CartItemModel>> = _cartItems

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        loadCartFromPreferences()
    }

    /** Reconstrói a lista de CartItemModel a partir do SharedPreferences */
    private fun loadCartFromPreferences() {
        viewModelScope.launch {
            val cartMap = withContext(Dispatchers.IO) {
                ShoppingCartUtils.getCartMap(appContext) // Map<productId, qty>
            }

            if (cartMap.isEmpty()) {
                _cartItems.value = emptyList()
                updateUiStateFromPrefs(emptyList())
                return@launch
            }

            val items = mutableListOf<CartItemModel>()

            for ((productId, qty) in cartMap) {
                if (qty <= 0) continue

                val product: ProductModel? = withContext(Dispatchers.IO) {
                    getProductByIdUseCase(productId)
                }

                if (product != null) {
                    items += CartItemModel(product = product, quantity = qty)
                } else {
                    // Se o produto não existir mais, opcionalmente limpar esse ID do carrinho
                    // ShoppingCartUtils.clearProduct(appContext, productId, 0L)
                }
            }

            _cartItems.value = items
            updateUiStateFromPrefs(items)
        }
    }

    /** Lê totais salvos no ShoppingCartUtils e monta o CartUiState */
    private fun updateUiStateFromPrefs(currentItems: List<CartItemModel>) {
        val totalWithout = ShoppingCartUtils.getTotalWithoutCommission(appContext)
        val totalWith = ShoppingCartUtils.getTotalWithCommission(appContext)
        val totalCommission = ShoppingCartUtils.getTotalCommission(appContext)

        val totalQty =
            if (currentItems.isNotEmpty()) currentItems.sumOf { it.quantity }
            else ShoppingCartUtils.getTotalQuantity(appContext)

        _uiState.value = CartUiState(
            items = currentItems,
            totalQuantity = totalQty,
            totalWithoutCommission = totalWithout,
            totalCommission = totalCommission,
            totalWithCommission = totalWith,
            isEmpty = (totalQty <= 0 || totalWith <= 0L)
        )
    }

    // ---------- Operações de carrinho (usando ShoppingCartUtils + estado em memória) ----------

    fun addProduct(product: ProductModel) {
        viewModelScope.launch {
            ShoppingCartUtils.addProduct(appContext, product.id, product.price)

            val currentList = _cartItems.value
            val existing = currentList.find { it.product.id == product.id }
            val newList = if (existing != null) {
                currentList.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                currentList + CartItemModel(product, 1)
            }
            _cartItems.value = newList
            updateUiStateFromPrefs(newList)
        }
    }

    fun removeProduct(product: ProductModel) {
        viewModelScope.launch {
            ShoppingCartUtils.removeOneProduct(appContext, product.id, product.price)

            val currentList = _cartItems.value
            val existing = currentList.find { it.product.id == product.id }
            val newList = if (existing != null && existing.quantity > 1) {
                currentList.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity - 1) else it
                }
            } else {
                currentList.filterNot { it.product.id == product.id }
            }
            _cartItems.value = newList
            updateUiStateFromPrefs(newList)
        }
    }

    fun reloadCartFromPrefs() {
        loadCartFromPreferences()
    }

    fun removeAllProductItems(product: ProductModel) {
        viewModelScope.launch {
            ShoppingCartUtils.clearProduct(appContext, product.id, product.price)
            val newList = _cartItems.value.filterNot { it.product.id == product.id }
            _cartItems.value = newList
            updateUiStateFromPrefs(newList)
        }
    }

    fun updateProductQuantity(productId: String, quantity: Int, price: Long) {
        viewModelScope.launch {
            if (quantity <= 0) {
                ShoppingCartUtils.clearProduct(appContext, productId, price)
                val newList = _cartItems.value.filterNot { it.product.id == productId }
                _cartItems.value = newList
                updateUiStateFromPrefs(newList)
                return@launch
            }

            val currentQty = ShoppingCartUtils.getProductQuantity(appContext, productId)
            val diff = quantity - currentQty

            if (diff > 0) {
                repeat(diff) {
                    ShoppingCartUtils.addProduct(appContext, productId, price)
                }
            } else if (diff < 0) {
                repeat(-diff) {
                    ShoppingCartUtils.removeOneProduct(appContext, productId, price)
                }
            }

            val newList = _cartItems.value.map {
                if (it.product.id == productId) it.copy(quantity = quantity) else it
            }
            _cartItems.value = newList
            updateUiStateFromPrefs(newList)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            ShoppingCartUtils.clearCart(appContext)
            _cartItems.value = emptyList()
            updateUiStateFromPrefs(emptyList())
        }
    }

    fun getTotalPrice(): Long {
        return _cartItems.value.sumOf { it.product.price * it.quantity }
    }

    fun getTotalQuantity(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }
}