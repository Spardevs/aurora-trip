package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale
import br.com.ticpass.pos.R

class PaymentSelectionActivity : BaseActivity() {
    private var totalValue: Double = 0.0
    private var remainingValue: Double = 0.0
    private var progress: String = ""
    private var isMultiPayment: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_selection)

        totalValue = intent.getDoubleExtra("total_value", 0.0)
        remainingValue = intent.getDoubleExtra("remaining_value", totalValue)
        progress = intent.getStringExtra("progress") ?: ""
        isMultiPayment = intent.getBooleanExtra("is_multi_payment", false)

        findViewById<TextView>(R.id.tv_value_to_pay).text =
            NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(remainingValue)

        findViewById<TextView>(R.id.tv_progress).text = "Pagamento $progress"

        findViewById<View>(R.id.btn_credit).setOnClickListener {
            confirmPayment("CREDITO", remainingValue)
        }
        findViewById<View>(R.id.btn_debit).setOnClickListener {
            confirmPayment("DEBITO", remainingValue)
        }
        findViewById<View>(R.id.btn_pix).setOnClickListener {
            confirmPayment("PIX", remainingValue)
        }
        findViewById<View>(R.id.btn_money).setOnClickListener {
            confirmPayment("DINHEIRO", remainingValue)
        }
    }

    private fun confirmPayment(method: String, value: Double) {
        val paymentType = when (method) {
            "CREDITO" -> "credit_card"
            "DEBITO" -> "debit_card"
            "PIX" -> "pix"
            "DINHEIRO" -> "cash"
            else -> throw IllegalArgumentException("Método inválido")
        }

        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("payment_type", paymentType)
            putExtra("value_to_pay", value) // Valor dividido
            putExtra("total_value", totalValue) // Valor total original
            putExtra("remaining_value", value) // Valor restante (neste caso, é o valor dividido)
            putExtra("is_multi_payment", isMultiPayment)
            putExtra("progress", progress)
        }
        startActivityForResult(intent, SplitEqualActivity.REQUEST_PAYMENT)
    }
}