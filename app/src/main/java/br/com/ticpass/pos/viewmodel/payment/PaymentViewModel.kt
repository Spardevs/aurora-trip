package br.com.ticpass.pos.viewmodel.payment

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONObject
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class PaymentCart(
    val totalItems: Int,
    val totalProductsValue: BigInteger,
    val totalCommission: BigInteger,
    val totalPrice: BigInteger
) {
    fun formattedTotalProductsValue(): String = formatCurrency(totalProductsValue)
    fun formattedTotalCommission(): String = formatCurrency(totalCommission)
    fun formattedTotalPrice(): String = formatCurrency(totalPrice)

    private fun formatCurrency(valueInCents: BigInteger): String {
        val valueInReais = valueInCents.toDouble() / 100.0
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(valueInReais)
    }
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val sharedPrefs: SharedPreferences,
    private val shoppingCartManager: ShoppingCartManager
) : ViewModel() {
    private val _cartData = MutableLiveData<PaymentCart>()
    val cartData: LiveData<PaymentCart> = _cartData

    private val paymentQueue = mutableListOf<SystemPaymentMethod>()
    private val _paymentTrigger = MutableLiveData<SystemPaymentMethod?>()
    private val _queueLiveData = MutableLiveData<List<SystemPaymentMethod>>(emptyList())
    val queueLiveData: LiveData<List<SystemPaymentMethod>> = _queueLiveData
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

                val totalItems = itemsObject.keys().asSequence().sumOf { key ->
                    itemsObject.getInt(key)
                }

                val totalProductsValue = jsonObject.optLong("totalProductsValue", 0L).toBigInteger()
                val totalCommission = jsonObject.optLong("totalCommission", 0L).toBigInteger()
                val totalPrice = jsonObject.optLong("totalPrice", 0L).toBigInteger()

                _cartData.postValue(
                    PaymentCart(
                        totalItems = totalItems,
                        totalProductsValue = totalProductsValue,
                        totalCommission = totalCommission,
                        totalPrice = totalPrice
                    )
                )
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
            PaymentMethod("Cartão de Débito", R.drawable.debit),
            PaymentMethod("Debug", R.drawable.icon)
        )
        _paymentMethods.postValue(methods)
    }

    fun clearCart() {
        shoppingCartManager.clearCart()
        loadCartData()
    }

    fun getCurrentPaymentValue(): SystemPaymentMethod? {
        return _paymentTrigger.value
    }
}

data class PaymentMethod(
    val name: String,
    val iconRes: Int,
    val value: String? = null
)