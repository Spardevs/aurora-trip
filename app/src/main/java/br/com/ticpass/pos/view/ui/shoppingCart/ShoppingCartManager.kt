package br.com.ticpass.pos.view.ui.shoppingCart

import android.content.Context
import androidx.core.content.edit

object ShoppingCartManager {
    private const val PREF_NAME = "ShoppingCart"
    private const val KEY_CART = "cart_items"

    fun addItem(context: Context, productId: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(productId.toString(), 0)
        prefs.edit { putInt(productId.toString(), current + 1) }
    }

    fun getQuantity(context: Context, productId: Int): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(productId.toString(), 0)
    }
}