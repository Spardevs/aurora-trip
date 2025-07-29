package br.com.ticpass.pos.viewmodel.payment

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import org.json.JSONObject
import androidx.core.content.edit

data class PaymentCart(
    val totalItems: Int,
    val totalPrice: Double
)

class PaymentViewModel(
    private val sharedPrefs: SharedPreferences,
    private val shoppingCartManager: ShoppingCartManager
) : ViewModel() {
    private val _cartData = MutableLiveData<PaymentCart>()
    val cartData: LiveData<PaymentCart> = _cartData

    val cartUpdates = MutableLiveData<Unit>()

    fun notifyCartUpdated() {
        cartUpdates.postValue(Unit)
    }


    private val _paymentMethods = MutableLiveData<List<PaymentMethod>>()
    val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    init {
        loadCartData()
        loadPaymentMethods()
    }

    fun loadCartData() {
        val jsonString = sharedPrefs.getString("shopping_cart_data", null)
        jsonString?.let {
            try {
                val jsonObject = JSONObject(it)
                val itemsObject = jsonObject.getJSONObject("items")

                // Calcula o total de itens
                val totalItems = itemsObject.keys().asSequence().sumOf { key ->
                    itemsObject.getInt(key)
                }

                val totalPrice = jsonObject.getDouble("totalPrice")

                _cartData.postValue(PaymentCart(totalItems, totalPrice))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadPaymentMethods() {
        val methods = listOf(
            PaymentMethod("Cartão de Crédito", R.drawable.credit),
            PaymentMethod("Pix", R.drawable.pix),
            PaymentMethod("Vale Refeição", R.drawable.vr),
            PaymentMethod("Cartão de Débito", R.drawable.debit)
        )
        _paymentMethods.postValue(methods)
    }

    fun clearCart() {
        shoppingCartManager.clearCart()
        loadCartData()
    }
}

data class PaymentMethod(
    val name: String,
    val iconRes: Int
)