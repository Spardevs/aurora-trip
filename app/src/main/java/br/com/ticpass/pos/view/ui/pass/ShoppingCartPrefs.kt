package br.com.ticpass.pos.view.ui.pass

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import javax.inject.Inject

class ShoppingCartPrefs @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "ShoppingCartPrefs"
        private const val KEY_SHOPPING_CART = "shopping_cart_data"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    fun getShoppingCartData(): ShoppingCartData? {
        val json = prefs.getString(KEY_SHOPPING_CART, null)
        return gson.fromJson(json, ShoppingCartData::class.java)
    }

    data class ShoppingCartData(
        val items: Map<String, Int> = emptyMap(),
        val observations: Map<String, String> = emptyMap(),
        val totalPrice: Long = 0
    )
}