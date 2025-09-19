package br.com.ticpass.pos.view.ui.shoppingCart

import android.annotation.SuppressLint
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
import androidx.lifecycle.LiveData
import br.com.ticpass.pos.data.activity.ShoppingCartActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import java.util.UUID

@Singleton
class ShoppingCartManager @Inject constructor(
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context
) {

    private val _cartUpdates = MutableLiveData<Unit>()
    val cartUpdates: LiveData<Unit> = _cartUpdates
    private val gson = Gson()
    private val shoppingCartPrefsName = "ShoppingCartPrefs"
    private val shoppingCartKey = "shopping_cart_data"
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(shoppingCartPrefsName, Context.MODE_PRIVATE)
    }

    private val observerMap = mutableMapOf<String, WeakReference<androidx.lifecycle.Observer<Any>>>()

    data class ShoppingCart(
        val items: Map<String, Int> = emptyMap(), // productId to quantity
        val totalPrice: BigInteger = BigInteger.ZERO,
        val observations: Map<String, String> = emptyMap() // productId to observation
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

    fun deleteItem(productId: String) {
        val items = currentCart.items.toMutableMap()
        items.remove(productId)
        val observations = currentCart.observations.toMutableMap()
        observations.remove(productId)

        val totalPrice = calculateTotalPrice(items)
        currentCart = ShoppingCart(items, totalPrice, observations)
        saveCart(currentCart)
        _cartUpdates.postValue(Unit)
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
            val product = runBlocking { productRepository.getById(productId) }
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

    /**
     * Método seguro para observeForever com gerenciamento de memória
     */
    fun observeForeverSafe(observer: androidx.lifecycle.Observer<Any>): String {
        val observerId = UUID.randomUUID().toString()
        _cartUpdates.observeForever(observer)
        observerMap[observerId] = WeakReference(observer)
        return observerId
    }

    /**
     * Remove um observer seguro
     */
    fun removeSafeObserver(observerId: String) {
        observerMap[observerId]?.get()?.let { observer ->
            _cartUpdates.removeObserver(observer)
        }
        observerMap.remove(observerId)
    }

    /**
     * Remove todos os observers
     */
    fun removeAllObservers() {
        observerMap.values.forEach { weakRef ->
            weakRef.get()?.let { observer ->
                _cartUpdates.removeObserver(observer)
            }
        }
        observerMap.clear()
    }

    /**
     * Limpa completamente o carrinho e todas as referências
     */
    fun clear() {
        currentCart = ShoppingCart()
        sharedPreferences.edit { remove(shoppingCartKey) }
        removeAllObservers()
        _cartUpdates.value = Unit
    }
}