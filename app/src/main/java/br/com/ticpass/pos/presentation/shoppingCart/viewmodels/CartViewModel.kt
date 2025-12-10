package br.com.ticpass.pos.presentation.shoppingCart.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.core.util.ShoppingCartUtils
import br.com.ticpass.pos.domain.product.model.ProductModel
import br.com.ticpass.pos.domain.shoppingCart.model.CartItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val _cartItems = MutableStateFlow<List<CartItemModel>>(emptyList())
    val cartItems: StateFlow<List<CartItemModel>> = _cartItems

    private val appContext: Context = application.applicationContext

    init {
        loadCartFromPreferences()
    }

    private fun loadCartFromPreferences() {
        viewModelScope.launch {
            val cartMap = ShoppingCartUtils.getCartMap(appContext)
            // Aqui voc\xea precisaria converter o mapa de ID->quantidade em uma lista de CartItemModel
            // Isso requer acesso ao reposit\xF3rio de produtos para obter os detalhes completos
            // Por enquanto, vamos manter o estado inicial vazio e atualizar conforme as opera\xE7\xF5es ocorrem
        }
    }

    fun addProduct(product: ProductModel) {
        viewModelScope.launch {
            // Atualiza o SharedPreferences
            ShoppingCartUtils.addProduct(appContext, product.id, product.price)

            // Atualiza o estado em mem\xF3ria
            val currentList = _cartItems.value
            val existing = currentList.find { it.product.id == product.id }
            if (existing != null) {
                _cartItems.value = currentList.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                _cartItems.value = currentList + CartItemModel(product, 1)
            }
        }
    }

    fun removeProduct(product: ProductModel) {
        viewModelScope.launch {
            // Atualiza o SharedPreferences
            ShoppingCartUtils.removeOneProduct(appContext, product.id, product.price)

            // Atualiza o estado em mem\xF3ria
            val currentList = _cartItems.value
            val existing = currentList.find { it.product.id == product.id }
            if (existing != null && existing.quantity > 1) {
                _cartItems.value = currentList.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity - 1) else it
                }
            } else {
                _cartItems.value = currentList.filterNot { it.product.id == product.id }
            }
        }
    }

    fun removeAllProductItems(product: ProductModel) {
        viewModelScope.launch {
            ShoppingCartUtils.clearProduct(appContext, product.id, product.price)
            _cartItems.value = _cartItems.value.filterNot { it.product.id == product.id }
        }
    }

    fun updateProductQuantity(productId: String, quantity: Int, price: Long) {
        viewModelScope.launch {
            if (quantity <= 0) {
                // Limpa completamente o produto do carrinho
                ShoppingCartUtils.clearProduct(appContext, productId, price)

                // Atualiza o estado em mem\xF3ria
                _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
                return@launch
            }

            // Atualiza a quantidade no SharedPreferences
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

            // Atualiza o estado em mem\xF3ria
            _cartItems.value = _cartItems.value.map {
                if (it.product.id == productId) it.copy(quantity = quantity) else it
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            // Limpa o SharedPreferences
            ShoppingCartUtils.clearCart(appContext)

            // Limpa o estado em mem\xF3ria
            _cartItems.value = emptyList()
        }
    }

    fun getTotalPrice(): Long {
        return _cartItems.value.sumOf { it.product.price * it.quantity }
    }

    fun getTotalQuantity(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }
}