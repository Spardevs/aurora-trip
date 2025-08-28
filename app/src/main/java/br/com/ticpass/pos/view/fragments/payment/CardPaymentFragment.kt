package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CardPaymentFragment : Fragment() {
    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    private var paymentType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentType = arguments?.getString("payment_type")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (::shoppingCartManager.isInitialized) {
            setupUI()
        } else {
            Log.e("CardPaymentFragment", "ShoppingCartManager not initialized")
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::shoppingCartManager.isInitialized) {
            enqueuePayment()
        }
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }

    private fun setupUI() {
        view?.let {
            val titleTextView = it.findViewById<TextView>(R.id.payment_form)
            val statusTextView = it.findViewById<TextView>(R.id.payment_status)
            val infoTextView = it.findViewById<TextView>(R.id.payment_info)
            val imageView = it.findViewById<android.widget.ImageView>(R.id.image)
            val priceTextView = it.findViewById<android.widget.TextView>(R.id.payment_price)

            // Acesse o cart apenas aqui, quando shoppingCartManager estiver inicializado
            val cart = shoppingCartManager.getCart()

            when (paymentType) {
                "credit_card" -> {
                    titleTextView?.text = "Cartão de Crédito"
                    statusTextView?.text = "Aguardando pagamento no crédito"
                    infoTextView?.text = "Aproxime, insira ou passe o cartão de crédito"
                    imageView?.setImageResource(R.drawable.ic_credit_card)
                    priceTextView?.text = formatCurrency(cart.totalPrice.toDouble())
                }
                "debit_card" -> {
                    titleTextView?.text = "Cartão de Débito"
                    statusTextView?.text = "Aguardando pagamento no débito"
                    infoTextView?.text = "Aproxime, insira ou passe o cartão de débito"
                    imageView?.setImageResource(R.drawable.ic_credit_card)
                    priceTextView?.text = formatCurrency(cart.totalPrice.toDouble())
                }
            }

            val cancelButton = it.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_pay)
            cancelButton?.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    private fun enqueuePayment() {
        val method = when (paymentType) {
            "credit_card" -> SystemPaymentMethod.CREDIT
            "debit_card" -> SystemPaymentMethod.DEBIT
            else -> return
        }

        // Acesse o cart apenas aqui
        val cart = shoppingCartManager.getCart()
        val amount = cart.totalPrice
        val commission = 0

        paymentViewModel.enqueuePayment(
            amount = amount.toInt(),
            commission = commission,
            method = method,
            isTransactionless = false
        )
    }
}