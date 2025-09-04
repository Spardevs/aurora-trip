package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import br.com.ticpass.pos.feature.payment.PaymentState
import br.com.ticpass.pos.payment.events.FinishPaymentHandler
import br.com.ticpass.pos.payment.events.PaymentType
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CardPaymentFragment : Fragment() {

    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    @Inject
    lateinit var finishPaymentHandler: FinishPaymentHandler

    private var paymentType: String? = null
    private var shouldStartImmediately = false
    private lateinit var titleTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var cancelButton: MaterialButton
    private lateinit var retryButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(
            appContext = requireContext(),
        )
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
        retryButton = view.findViewById(R.id.btn_retry)

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
                    Log.d("PaymentState", "Estado atual: $state")
                    when (state) {
                        is PaymentState.Processing -> {
                            updateUIForProcessing()
                        }
                        is PaymentState.Success -> {
                            handleSuccessfulPayment(state)
                            updateUIForSuccess()
                        }
                        is PaymentState.Error -> {
                            updateUIForError(state.errorMessage)
                        }
                        is PaymentState.Cancelled -> {
                            updateUIForCancelled()
                        }
                        is PaymentState.Idle -> {
                            // Estado ocioso
                        }
                        is PaymentState.Initializing -> {
                            statusTextView.text = "Inicializando sistema de pagamento..."
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

        Log.d("CardPayment", "Enfileirando pagamento: $amount, método: $method")

        paymentViewModel.enqueuePayment(
            amount = amount.toInt(),
            commission = commission,
            method = method,
            isTransactionless = true
        )

        if (startImmediately) {
            Log.d("CardPayment", "Iniciando processamento imediato")
            paymentViewModel.startProcessing()
        }
    }

    private fun updateUIForProcessing() {
        statusTextView.text = "Processando pagamento..."
        infoTextView.text = "Aguarde, estamos processando sua transação"
    }

    private fun updateUIForSuccess() {
        activity?.runOnUiThread {
            statusTextView.text = "Pagamento Aprovado!"
            infoTextView.text = "Transação concluída com sucesso"
            imageView.setImageResource(R.drawable.ic_check)

            cancelButton.text = "Finalizar"
            cancelButton.setOnClickListener {
                shoppingCartManager.clearCart()
                requireActivity().finish()
            }
            retryButton.visibility = View.GONE

            imageView.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(500)
                .withEndAction {
                    imageView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                }
                .start()
        }
    }

    private fun updateUIForError(errorMessage: String) {
        activity?.runOnUiThread {
            statusTextView.text = "Erro no Pagamento"
            infoTextView.text = errorMessage
            imageView.setImageResource(R.drawable.ic_close)

            cancelButton.text = "Cancelar"
            cancelButton.setOnClickListener {
                paymentViewModel.abortAllPayments()
                requireActivity().finish()
            }
            retryButton.visibility = View.VISIBLE
            retryButton.text = "Tentar Novamente"
            retryButton.setOnClickListener {
                retryPayment()
            }
        }
    }

    private fun retryPayment() {
        activity?.runOnUiThread {
            retryButton.visibility = View.GONE
            statusTextView.text = "Preparando nova tentativa..."
            infoTextView.text = "Aguarde..."
            imageView.setImageResource(when (paymentType) {
                "credit_card" -> R.drawable.ic_credit_card
                "debit_card" -> R.drawable.ic_credit_card
                else -> R.drawable.ic_credit_card
            })
            imageView.clearColorFilter()

            Handler(Looper.getMainLooper()).postDelayed({
                enqueuePayment(startImmediately = true)
            }, 1000)
        }
    }

    private fun updateUIForCancelled() {
        statusTextView.text = "Pagamento Cancelado"
        infoTextView.text = "A transação foi cancelada"
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.Builder()
            .setLanguage("pt")
            .setRegion("BR")
            .build())
        return format.format(value)
    }

    private fun handleSuccessfulPayment(state: PaymentState.Success) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val paymentTypeEnum = when (paymentType) {
                    "credit_card", "debit_card" -> PaymentType.SINGLE_PAYMENT
                    else -> PaymentType.SINGLE_PAYMENT
                }
                val cart = shoppingCartManager.getCart()
                val amount = cart.totalPrice.toInt()
                val commission = 0
                val method = when (paymentType) {
                    "credit_card" -> SystemPaymentMethod.CREDIT
                    "debit_card" -> SystemPaymentMethod.DEBIT
                    else -> SystemPaymentMethod.CREDIT
                }
                val paymentData = PaymentUIUtils.PaymentData(
                    amount = amount,
                    commission = commission,
                    method = method,
                    isTransactionless = true
                )
                finishPaymentHandler.handlePayment(paymentTypeEnum, paymentData)
            } catch (e: Exception) {
                Log.e("CardPaymentFragment", "Erro ao processar pagamento finalizado: ${e.message}")
            }
        }
    }
}