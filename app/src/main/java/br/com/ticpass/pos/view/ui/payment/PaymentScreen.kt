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

@AndroidEntryPoint
class PaymentScreen : BaseActivity() {

    // ViewModel correto para dados do carrinho e totais
    private val paymentViewModel: PaymentViewModel by viewModels()

    // Se precisar do outro ViewModel para processamento, declare aqui
    private val paymentProcessingViewModel: PaymentProcessingViewModel by viewModels()

    private lateinit var tvTotalPrice: TextView
    private lateinit var tvSubTotal: TextView
    private lateinit var tvTotalCommission: TextView

    private lateinit var llSubTotal: LinearLayout
    private lateinit var llTotalPrice: LinearLayout
    private lateinit var llTotalCommission: LinearLayout

    private lateinit var cartContainer: LinearLayout

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

        if (!showCartButton) {
            cartContainer.visibility = View.GONE
            llSubTotal.visibility = View.VISIBLE
            llTotalCommission.visibility = View.VISIBLE
        } else {
            cartContainer.visibility = View.VISIBLE
        }
        cartContainer.setOnClickListener {
            val intent = Intent(this, ShoppingCartScreen::class.java)
            startActivityForResult(intent, ProductsListScreen.REQUEST_CART_UPDATE)
        }

        // Observa os dados do carrinho para atualizar os valores na UI
        paymentViewModel.cartData.observe(this) { cart ->
            tvTotalPrice.text = cart.formattedTotalPrice()
            if (!showCartButton) {
                tvSubTotal.text = "Produtos: ${cart.formattedTotalProductsValue()}"
                tvTotalCommission.text = "Comissão: ${cart.formattedTotalCommission()}"
            }
        }

        findViewById<View>(R.id.payment_container).post {
            loadFragment(paymentType)
        }
    }

    private fun loadFragment(paymentType: String) {
        Log.d("PaymentScreen", "Opening payment screen for type: $paymentType")

        val fragment = when (paymentType) {
            "credit_card", "debit_card" -> {
                CardPaymentFragment().apply {
                    arguments = Bundle().apply {
                        putString("payment_type", paymentType)
                    }
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
}