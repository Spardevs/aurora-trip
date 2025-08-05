package br.com.ticpass.pos.view.ui.shoppingCart

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import br.com.ticpass.pos.data.room.repository.ProductRepository
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import br.com.ticpass.pos.data.activity.ShoppingCartActivity
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ShoppingCartManager @Inject constructor(
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val shoppingCartPrefsName = "ShoppingCartPrefs"
    private val shoppingCartKey = "shopping_cart_data"
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(shoppingCartPrefsName, Context.MODE_PRIVATE)
    }

    private val _cartUpdates = MutableLiveData<Unit>()
    val cartUpdates = _cartUpdates

    data class ShoppingCart(
        val items: Map<String, Int> = emptyMap(),
        val totalPrice: BigInteger = BigInteger.ZERO,
        val observations: Map<String, String> = emptyMap()
    )

    private var currentCart: ShoppingCart = loadCart()

    fun getCart(): ShoppingCart = currentCart
    fun getQuantity(productId: String): Int = currentCart.items[productId] ?: 0

    fun addItem(productId: String) {
        val currentQuantity = currentCart.items[productId] ?: 0
        updateItem(productId, currentQuantity + 1)
    }

    fun removeItem(productId: String) {
        val currentQuantity = currentCart.items[productId] ?: 0
        if (currentQuantity > 0) {
            updateItem(productId, currentQuantity - 1)
        }
    }

    fun getObservation(productId: String): String? = currentCart.observations[productId]

    fun updateObservation(productId: String, observation: String) {
        val observations = currentCart.observations.toMutableMap()
        if (observation.isNotEmpty()) {
            observations[productId] = observation
        } else {
            observations.remove(productId)
        }

        currentCart = currentCart.copy(observations = observations)
        saveCart(currentCart)
        _cartUpdates.postValue(Unit)
    }


    fun getAllItems(activity: ShoppingCartActivity): Map<String, Int> = currentCart.items

    fun notifyCartUpdated() {
        _cartUpdates.postValue(Unit)
    }

    fun updateItem(productId: String, newQuantity: Int) {
        val items = currentCart.items.toMutableMap()

        if (newQuantity > 0) {
            items[productId] = newQuantity
        } else {
            items.remove(productId)
        }

        val totalPrice = calculateTotalPrice(items)
        currentCart = ShoppingCart(items, totalPrice)
        saveCart(currentCart)
        _cartUpdates.postValue(Unit)
    }

    fun clearCart() {
        currentCart = ShoppingCart()
        saveCart(currentCart)
        _cartUpdates.postValue(Unit)
    }

    private fun calculateTotalPrice(items: Map<String, Int>): BigInteger {
        return items.entries.fold(BigInteger.ZERO) { total, (productId, quantity) ->
            val product = runBlocking { productRepository.getById(productId) }  // No need for toString()
            total + (product?.price?.toBigInteger() ?: BigInteger.ZERO) * quantity.toBigInteger()
        }
    }

    private fun saveCart(cart: ShoppingCart) {
        sharedPreferences.edit {
            putString(shoppingCartKey, gson.toJson(cart))
        }
    }

    private fun loadCart(): ShoppingCart {
        val cartJson = sharedPreferences.getString(shoppingCartKey, null) ?: return ShoppingCart()
        return try {
            gson.fromJson(cartJson, ShoppingCart::class.java) ?: ShoppingCart()
        } catch (e: Exception) {
            ShoppingCart()
        }
    }

    fun getTotalItemsCount(): Int = currentCart.items.values.sum()




}