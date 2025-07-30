package br.com.ticpass.pos.view.ui.payment

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R

class PaymentScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val paymentType = intent.getStringExtra("payment_type") ?: "Pagamento"
        val textView = findViewById<TextView>(R.id.textView2)
        textView.text = "Forma de pagamento: $paymentType"
    }
}
