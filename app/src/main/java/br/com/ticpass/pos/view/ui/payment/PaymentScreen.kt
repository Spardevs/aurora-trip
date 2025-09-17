package br.com.ticpass.pos.view.ui.payment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.PaymentProcessingActivity
import br.com.ticpass.pos.view.fragments.payment.CashPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.CardPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.PixPaymentFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentScreen : BaseActivity() {

    private val paymentViewModel: PaymentProcessingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentType = intent.getStringExtra("payment_type") ?: run {
            Log.e("PaymentScreen", "Payment type is null or not provided")
            return
        }

        if (paymentType == "debug") {
            startActivity(Intent(this, PaymentProcessingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_payment)

        findViewById<android.view.View>(R.id.payment_container).post {
            loadFragment(paymentType)
        }
    }

    private fun loadFragment(paymentType: String) {
        Log.d("PaymentScreen", "Opening payment screen for type: $paymentType")

        val fragment = when (paymentType) {
            "credit_card", "debit_card" -> {
                val cardFragment = CardPaymentFragment()
                val args = Bundle()
                args.putString("payment_type", paymentType)
                cardFragment.arguments = args
                cardFragment
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