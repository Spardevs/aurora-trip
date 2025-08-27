package br.com.ticpass.pos.view.ui.payment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.PaymentEnqueuer
import br.com.ticpass.pos.payment.PaymentProcessingActivity
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.sdk.payment.PaymentProvider
import br.com.ticpass.pos.view.fragments.payment.CardPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.CashPaymentFragment
import br.com.ticpass.pos.view.fragments.payment.PixPaymentFragment
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentScreen : BaseActivity(), PaymentEnqueuer {

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    override val paymentViewModel: PaymentProcessingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Inicializa o PaymentProvider se não estiver inicializado
        if (!PaymentProvider.isInitialized()) {
            PaymentProvider.initialize(appContext = applicationContext)
            // Ou: PaymentProvider.initialize(applicationContext)
        }

        val paymentType = intent.getStringExtra("payment_type") ?: run {
            Log.e("PaymentScreen", "Payment type is null or not provided")
            finish()
            return
        }

        if (paymentType == "debug") {
            startActivity(Intent(this, PaymentProcessingActivity::class.java))
            finish()
            return
        }

        val fragment = when (paymentType) {
            "credit_card" -> CardPaymentFragment.newInstance(SystemPaymentMethod.CREDIT)
            "debit_card" -> CardPaymentFragment.newInstance(SystemPaymentMethod.DEBIT)
            "pix" -> PixPaymentFragment()
            "cash" -> CashPaymentFragment()
            else -> {
                Log.e("PaymentScreen", "Invalid payment type: $paymentType")
                finish()
                return
            }
        }

        supportFragmentManager.commit {
            replace(R.id.payment_container, fragment)
            addToBackStack(null)
        }
    }

    override fun enqueuePayment(method: SystemPaymentMethod) {
        try {
            val paymentData = PaymentUIUtils.createPaymentData(
                method = method,
                isTransactionlessEnabled = false,
                amount = 1000,
                commission = 10
            )

            paymentViewModel.enqueuePayment(
                amount = paymentData.amount,
                commission = paymentData.commission,
                method = paymentData.method,
                isTransactionless = paymentData.isTransactionless
            )
        } catch (e: IllegalStateException) {
            Log.e("PaymentScreen", "Erro ao enfileirar pagamento: ${e.message}")
            Toast.makeText(this, "Erro no sistema de pagamento", Toast.LENGTH_SHORT).show()
        }
    }
}
