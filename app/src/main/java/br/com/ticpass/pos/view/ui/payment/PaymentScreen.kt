package br.com.ticpass.pos.view.ui.payment

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.fragments.payment.CashPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.CreditCardPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.DebitCardPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.PixPaymentFragment
import java.util.Locale

class PaymentScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)



        val paymentType = intent.getStringExtra("payment_type") ?: run {
            Log.e("PaymentScreen", "Payment type is null or not provided")
            return
        }

        Log.d("PaymentScreen", "Opening payment screen for type: $paymentType")


        val fragment = when (paymentType) {
            "credit_card" -> CreditCardPaymentFragment()
            "debit_card" -> DebitCardPaymentFragment()
            "pix" -> PixPaymentFragment()
            "cash" -> CashPaymentFragment()
            else -> return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.payment_container, fragment)
            .commitNow()
    }
}