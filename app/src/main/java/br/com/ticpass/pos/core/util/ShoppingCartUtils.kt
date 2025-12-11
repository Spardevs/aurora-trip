package br.com.ticpass.pos.core.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong
import androidx.core.content.edit

object ShoppingCartUtils {

    private const val PREFS_NAME = "ShoppingCart"
    private const val CART_KEY = "cart" // JSON string: { "productId": qty, ... }

    // Novos totais
    private const val TOTAL_COMMISSION_KEY = "total_commission" // Long (cents)
    private const val TOTAL_WITH_COMMISSION_KEY = "total_with_commission" // Long (cents)
    private const val TOTAL_WITHOUT_COMMISSION_KEY = "total_without_commission" // Long (cents)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getCartJson(context: Context): JSONObject {
        val json = prefs(context).getString(CART_KEY, "{}") ?: "{}"
        return JSONObject(json)
    }

    private fun saveCartJson(context: Context, json: JSONObject) {
        prefs(context).edit { putString(CART_KEY, json.toString()) }
    }

    fun getProductQuantity(context: Context, productId: String): Int {
        val cart = getCartJson(context)
        return cart.optInt(productId, 0)
    }

    fun getTotalQuantity(context: Context): Int {
        val cart = getCartJson(context)
        var total = 0
        val keys = cart.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            total += cart.optInt(key, 0)
        }
        return total
    }

    fun getCartMap(context: Context): Map<String, Int> {
        val cart = getCartJson(context)
        val map = mutableMapOf<String, Int>()
        val keys = cart.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = cart.optInt(key, 0)
        }
        return map
    }

    /** Total da comissão salva (já separado) */
    fun getTotalCommission(context: Context): Long {
        return prefs(context).getLong(TOTAL_COMMISSION_KEY, 0L)
    }

    /** Total com comissão */
    fun getTotalWithCommission(context: Context): Long {
        return prefs(context).getLong(TOTAL_WITH_COMMISSION_KEY, 0L)
    }

    /** Total sem comissão */
    fun getTotalWithoutCommission(context: Context): Long {
        return prefs(context).getLong(TOTAL_WITHOUT_COMMISSION_KEY, 0L)
    }

    /**
     * Salva:
     * - totalWithout: total sem comissão
     * - totalWith: total com comissão
     * - commission: diferença (totalWith - totalWithout)
     */
    private fun saveTotals(context: Context, totalWithout: Long, totalWith: Long) {
        val safeTotalWithout = totalWithout.coerceAtLeast(0L)
        val safeTotalWith = totalWith.coerceAtLeast(0L)
        val commission = (safeTotalWith - safeTotalWithout).coerceAtLeast(0L)

        prefs(context).edit {
            putLong(TOTAL_WITHOUT_COMMISSION_KEY, safeTotalWithout)
            putLong(TOTAL_WITH_COMMISSION_KEY, safeTotalWith)
            putLong(TOTAL_COMMISSION_KEY, commission)
        }
    }

    // Recalcula apenas o total com comissão a partir do totalWithout (suspend porque usa CommisionUtils)
    private suspend fun recalcTotalWithCommission(totalWithout: Long): Long {
        // compute using CommisionUtils (suspend)
        val totalWithDouble = CommisionUtils.calculateTotalWithCommission(totalWithout)
        return totalWithDouble.roundToLong()
    }

    // Adiciona 1 unidade do produto (price está em centavos como Long)
    suspend fun addProduct(context: Context, productId: String, price: Long) {
        withContext(Dispatchers.IO) {
            val prefs = prefs(context)
            val cart = getCartJson(context)

            val currentQty = cart.optInt(productId, 0)
            val newQty = currentQty + 1
            cart.put(productId, newQty)
            saveCartJson(context, cart)

            // Atualizar totalWithout (sem comissão)
            val currentTotalWithout = prefs.getLong(TOTAL_WITHOUT_COMMISSION_KEY, 0L)
            val newTotalWithout = currentTotalWithout + price
            val newTotalWith = recalcTotalWithCommission(newTotalWithout)

            saveTotals(context, newTotalWithout, newTotalWith)
        }
    }

    // Remove 1 unidade do produto (se qty > 1 decrementa, se = 1 remove)
    // Se o produto não existir nada acontece.
    suspend fun removeOneProduct(context: Context, productId: String, price: Long) {
        withContext(Dispatchers.IO) {
            val prefs = prefs(context)
            val cart = getCartJson(context)

            if (!cart.has(productId)) return@withContext

            val currentQty = cart.optInt(productId, 0)
            if (currentQty > 1) {
                cart.put(productId, currentQty - 1)
            } else {
                cart.remove(productId)
            }
            saveCartJson(context, cart)

            val currentTotalWithout = prefs.getLong(TOTAL_WITHOUT_COMMISSION_KEY, 0L)
            val newTotalWithout = (currentTotalWithout - price).coerceAtLeast(0L)
            val newTotalWith = recalcTotalWithCommission(newTotalWithout)

            saveTotals(context, newTotalWithout, newTotalWith)
        }
    }

    // Limpar completamente o produto do carrinho (ou seja, remover todas as quantidades)
    // Subtrai price * qty do totalWithout
    suspend fun clearProduct(context: Context, productId: String, price: Long) {
        withContext(Dispatchers.IO) {
            val prefs = prefs(context)
            val cart = getCartJson(context)

            if (!cart.has(productId)) return@withContext

            val qty = cart.optInt(productId, 0)
            cart.remove(productId)
            saveCartJson(context, cart)

            val subtract = price * qty.toLong()
            val currentTotalWithout = prefs.getLong(TOTAL_WITHOUT_COMMISSION_KEY, 0L)
            val newTotalWithout = (currentTotalWithout - subtract).coerceAtLeast(0L)
            val newTotalWith = recalcTotalWithCommission(newTotalWithout)

            saveTotals(context, newTotalWithout, newTotalWith)
        }
    }

    // Limpar todo o carrinho
    suspend fun clearCart(context: Context) {
        withContext(Dispatchers.IO) {
            saveCartJson(context, JSONObject("{}"))
            // zera todos os totais
            saveTotals(context, 0L, 0L)
        }
    }
}