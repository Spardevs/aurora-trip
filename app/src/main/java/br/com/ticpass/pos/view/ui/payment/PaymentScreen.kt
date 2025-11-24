package br.com.ticpass.pos.view.ui.payment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.PaymentProcessingActivity
import br.com.ticpass.pos.view.fragments.payment.CashPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.CardPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.PixPaymentFragment
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartScreen
import br.com.ticpass.pos.viewmodel.payment.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class PaymentScreen : BaseActivity() {

    private val paymentViewModel: PaymentViewModel by viewModels()
    private val paymentProcessingViewModel: PaymentProcessingViewModel by viewModels()

    private lateinit var tvTotalPrice: TextView
    private lateinit var tvSubTotal: TextView
    private lateinit var tvTotalCommission: TextView

    private lateinit var llSubTotal: LinearLayout
    private lateinit var llTotalPrice: LinearLayout
    private lateinit var llTotalCommission: LinearLayout

    private var cartContainer: LinearLayout? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentType = intent.getStringExtra("payment_type") ?: run {
            Log.e("PaymentScreen", "Payment type is null or not provided")
            finish()
            return
        }

        val showCartButton = intent.getBooleanExtra("show_cart_button", true)

        if (paymentType == "debug") {
            startActivity(Intent(this, PaymentProcessingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_payment)

        tvTotalPrice = findViewById(R.id.tv_total_price)
        tvSubTotal = findViewById(R.id.tv_sub_total)
        tvTotalCommission = findViewById(R.id.tv_total_commission)

        llSubTotal = findViewById(R.id.ll_sub_total)
        llTotalPrice = findViewById(R.id.ll_total_price)
        llTotalCommission = findViewById(R.id.ll_total_commission)

        cartContainer = findViewById(R.id.cart_container)

        if (cartContainer == null) {
            // Apenas log — não crashar. Verifique seu layout activity_payment para adicionar cart_container.
            Log.e("PaymentScreen", "cart_container view not found in activity_payment layout")
        } else {
            if (!showCartButton) {
                cartContainer!!.visibility = View.GONE
                llSubTotal.visibility = View.VISIBLE
                llTotalCommission.visibility = View.VISIBLE
            } else {
                cartContainer!!.visibility = View.VISIBLE
            }

            // Clique no container do carrinho -> abrir ShoppingCartScreen
            cartContainer!!.setOnClickListener {
                // Recomenda-se NÃO passar objetos grandes aqui. Ex.: NÃO: intent.putExtra("product", product)
                val intent = Intent(this, ShoppingCartScreen::class.java)
                startActivityForResult(intent, ProductsListScreen.REQUEST_CART_UPDATE)
            }
        }

        // Observa os dados do carrinho para atualizar UI
        paymentViewModel.cartData.observe(this) { cart ->
            tvTotalPrice.text = formatCurrency(cart.totalPrice)
            if (!showCartButton) {
                tvSubTotal.text = formatCurrency(cart.totalProductsValue)
                tvTotalCommission.text = formatCurrency(cart.totalCommission)
            }
        }

        findViewById<View>(R.id.payment_container).post {
            loadFragment(paymentType)
        }
    }

    private fun loadFragment(paymentType: String) {
        Log.d("PaymentScreen", "Opening payment screen for type: $paymentType")

        val fragment = when (paymentType) {
            "credit_card", "debit_card" -> CardPaymentFragment().apply {
                arguments = Bundle().apply {
                    putString("payment_type", paymentType)
                }
            }
            "pix" -> PixPaymentFragment().apply {
                arguments = Bundle().apply {
                    putString("payment_type", paymentType)
                }
            }
            "cash" -> CashPaymentFragment().apply {
                arguments = Bundle().apply {
                    putString("payment_type", paymentType)
                }
            }
            else -> {
                Log.e("PaymentScreen", "Unknown payment type: $paymentType")
                return
            }
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.payment_container, fragment)
            .commit()
    }

    private fun formatCurrency(valueInCents: BigInteger): String {
        // Assumi que valueInCents representa centavos (por ex. 2500 => R$25,00).
        // Ajuste o divisor conforme seu domínio. Se for centavos: /100. Se for outra unidade, ajuste.
        val valueInReais = valueInCents.toDouble() / 100.0
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(valueInReais)
    }
}