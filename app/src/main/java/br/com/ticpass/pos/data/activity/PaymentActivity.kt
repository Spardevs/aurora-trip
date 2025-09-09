package br.com.ticpass.pos.data.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.repository.PaymentRepository
import br.com.ticpass.pos.databinding.PaymentSheetBinding
import br.com.ticpass.pos.view.fragments.payment.CardPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.CashPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.PixPaymentFragment
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.viewmodel.payment.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {

    private lateinit var viewModel: PaymentViewModel
    private lateinit var binding: PaymentSheetBinding

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PaymentSheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obter os valores passados
        val paymentType = intent.getStringExtra("payment_type")
        val paymentValue = intent.getDoubleExtra("value_to_pay", 0.0)
        val totalValue = intent.getDoubleExtra("total_value", paymentValue)
        val remainingValue = intent.getDoubleExtra("remaining_value", paymentValue)
        val isMultiPayment = intent.getBooleanExtra("is_multi_payment", false)
        val progress = intent.getStringExtra("progress") ?: ""

        if (savedInstanceState == null) {
            val fragment = when (paymentType) {
                "credit_card", "debit_card" -> CardPaymentFragment().apply {
                    arguments = Bundle().apply {
                        putString("payment_type", paymentType)
                        putDouble("value_to_pay", paymentValue) // Valor dividido
                        putDouble("total_value", totalValue) // Valor total
                        putDouble("remaining_value", remainingValue) // Valor restante
                        putBoolean("is_multi_payment", isMultiPayment)
                        putString("progress", progress)
                    }
                }
                "pix" -> PixPaymentFragment().apply {
                    arguments = Bundle().apply {
                        putString("payment_type", paymentType)
                        putDouble("value_to_pay", paymentValue) // Valor dividido
                        putDouble("total_value", totalValue) // Valor total
                        putDouble("remaining_value", remainingValue) // Valor restante
                        putBoolean("is_multi_payment", isMultiPayment)
                        putString("progress", progress)
                    }
                }
                else -> CashPaymentFragment().apply {
                    arguments = Bundle().apply {
                        putString("payment_type", paymentType)
                        putDouble("value_to_pay", paymentValue) // Valor dividido
                        putDouble("total_value", totalValue) // Valor total
                        putDouble("remaining_value", remainingValue) // Valor restante
                        putBoolean("is_multi_payment", isMultiPayment)
                        putString("progress", progress)
                    }
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.payment_container, fragment)
                .commit()
        }

        val sharedPrefs = getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
        viewModel = ViewModelProvider(
            this,
            PaymentViewModelFactory(sharedPrefs, shoppingCartManager)
        )[PaymentViewModel::class.java]

        setupObservers()
    }

    private fun setupObservers() {
        shoppingCartManager.cartUpdates.observe(this) {
            Log.d("PaymentActivity", "CartUpdates observed - Updating UI")
            updateCartUI()
        }
    }

    private fun updateCartUI() {
        val cart = shoppingCartManager.getCart()
        Log.d("PaymentActivity", "Updating UI - Items: ${cart.items.values.sum()}, Total: ${cart.totalPrice}")

        binding.tvItemsCount.text = resources.getQuantityString(
            R.plurals.items_count,
            cart.items.values.sum(),
            cart.items.values.sum()
        )
        binding.tvTotalPrice.text = formatCurrency(cart.totalPrice.toDouble())
        binding.btnClearAll.visibility = if (cart.items.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }
}

class PaymentViewModelFactory(
    private val sharedPrefs: SharedPreferences,
    private val shoppingCartManager: ShoppingCartManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(sharedPrefs, shoppingCartManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

