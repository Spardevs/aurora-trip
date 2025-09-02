package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CardPaymentFragment : Fragment() {
    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    private var paymentType: String? = null
    private var shouldStartImmediately = false
    private lateinit var titleTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var cancelButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentType = arguments?.getString("payment_type")
        shouldStartImmediately = true
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
            setupUI(view)
            setupObservers()
        } else {
            Log.e("CardPaymentFragment", "ShoppingCartManager not initialized")
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::shoppingCartManager.isInitialized) {
            enqueuePayment(startImmediately = true)
        }
    }

    private fun setupUI(view: View) {
        titleTextView = view.findViewById(R.id.payment_form)
        statusTextView = view.findViewById(R.id.payment_status)
        infoTextView = view.findViewById(R.id.payment_info)
        imageView = view.findViewById(R.id.image)
        priceTextView = view.findViewById(R.id.payment_price)
        cancelButton = view.findViewById(R.id.btn_cancel)

        val cart = shoppingCartManager.getCart()

        when (paymentType) {
            "credit_card" -> {
                titleTextView.text = "Cartão de Crédito"
                statusTextView.text = "Aguardando pagamento no crédito"
                infoTextView.text = "Aproxime, insira ou passe o cartão de crédito"
                imageView.setImageResource(R.drawable.ic_credit_card)
                priceTextView.text = formatCurrency(cart.totalPrice.toDouble())
            }
            "debit_card" -> {
                titleTextView.text = "Cartão de Débito"
                statusTextView.text = "Aguardando pagamento no débito"
                infoTextView.text = "Aproxime, insira ou passe o cartão de débito"
                imageView.setImageResource(R.drawable.ic_credit_card)
                priceTextView.text = formatCurrency(cart.totalPrice.toDouble())
            }
        }

        cancelButton.setOnClickListener {
            paymentViewModel.abortAllPayments()
            requireActivity().finish()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.paymentState.collect { state ->
                    when (state) {
                        is PaymentProcessingViewModel.PaymentState.Processing -> {
                            updateUIForProcessing()
                        }
                        is PaymentProcessingViewModel.PaymentState.Success -> {
                            updateUIForSuccess(state.transactionId)
                        }
                        is PaymentProcessingViewModel.PaymentState.Error -> {
                            updateUIForError(state.errorMessage)
                        }
                        is PaymentProcessingViewModel.PaymentState.Cancelled -> {
                            updateUIForCancelled()
                        }
                        is PaymentProcessingViewModel.PaymentState.Idle -> {
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.paymentEvents.collect { event ->
                    when (event) {
                        is PaymentProcessingViewModel.PaymentEvent.CardDetected -> {
                            updateUIForCardDetected()
                        }
                        is PaymentProcessingViewModel.PaymentEvent.Processing -> {
                            updateUIForProcessing(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun enqueuePayment(startImmediately: Boolean) {
        val method = when (paymentType) {
            "credit_card" -> SystemPaymentMethod.CREDIT
            "debit_card" -> SystemPaymentMethod.DEBIT
            else -> return
        }

        val cart = shoppingCartManager.getCart()
        val amount = cart.totalPrice
        val commission = 0

        paymentViewModel.enqueuePayment(
            amount = amount.toInt(),
            commission = commission,
            method = method,
            isTransactionless = true
        )

        if (startImmediately) {
            paymentViewModel.startProcessing()
        }
    }

    private fun updateUIForProcessing() {
        statusTextView.text = "Processando pagamento..."
        infoTextView.text = "Aguarde, estamos processando sua transação"
    }

    private fun updateUIForProcessing(message: String) {
        statusTextView.text = "Processando..."
        infoTextView.text = message
    }

    private fun updateUIForSuccess(transactionId: String) {
        statusTextView.text = "Pagamento Aprovado!"
        infoTextView.text = "Transação: $transactionId\nObrigado pela compra!"
        imageView.setImageResource(R.drawable.ic_check)
        cancelButton.text = "Finalizar"
    }

    private fun updateUIForError(errorMessage: String) {
        statusTextView.text = "Erro no Pagamento"
        infoTextView.text = errorMessage
        imageView.setImageResource(R.drawable.ic_close)
        cancelButton.text = "Tentar Novamente"
        cancelButton.setOnClickListener {
            enqueuePayment(startImmediately = true)
        }
    }

    private fun updateUIForCancelled() {
        statusTextView.text = "Pagamento Cancelado"
        infoTextView.text = "A transação foi cancelada"
    }

    private fun updateUIForCardDetected() {
        infoTextView.text = "Cartão detectado! Processando..."
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }
}