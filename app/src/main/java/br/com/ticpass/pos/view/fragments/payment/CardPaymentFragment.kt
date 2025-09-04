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
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.events.PaymentType
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.TimeoutCountdownView // Import correto
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent // Import necessário
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
    private lateinit var paymentEventHandler: PaymentEventHandler

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

    // Adicione esta view para o countdown
    private lateinit var timeoutCountdownView: TimeoutCountdownView

    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(requireContext())
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
            setupPaymentEventHandler(view)
            setupObservers()
        } else {
            Log.e("CardPaymentFragment", "ShoppingCartManager not initialized")
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::shoppingCartManager.isInitialized) {
            enqueuePayment(startImmediately = true) // Corrigido: chama o método da classe
        }
    }

    private fun setupPaymentEventHandler(view: View) {
        // Encontre a view de countdown (você precisa adicionar ao layout)
        timeoutCountdownView = view.findViewById(R.id.timeout_countdown)

        paymentEventHandler = PaymentEventHandler(
            context = requireContext(),
            dialogEventTextView = infoTextView, // Usando infoTextView para mensagens
            dialogQRCodeImageView = imageView,  // Usando imageView para QR codes
            dialogTimeoutCountdownView = timeoutCountdownView
        )
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
        // Observer para eventos de processamento de pagamento
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.paymentProcessingEvents.collect { event ->
                    paymentEventHandler.handlePaymentEvent(event)

                    // Sincronize o statusTextView com eventos importantes
                    when (event) {
                        is PaymentProcessingEvent.CARD_REACH_OR_INSERT -> {
                            statusTextView.text = "Aproxime ou insira o cartão"
                        }

                        is PaymentProcessingEvent.USE_CHIP -> {
                            statusTextView.text = "Insira o cartão no chip"
                        }

                        is PaymentProcessingEvent.USE_MAGNETIC_STRIPE -> {
                            statusTextView.text = "Passe o cartão na tarja magnética"
                        }

                        is PaymentProcessingEvent.SWIPE_CARD_REQUESTED -> {
                            statusTextView.text = "Passe o cartão"
                        }

                        is PaymentProcessingEvent.CARD_INSERTED -> {
                            statusTextView.text = "Cartão inserido - processando..."
                        }

                        is PaymentProcessingEvent.PIN_REQUESTED -> {
                            statusTextView.text = "Insira o PIN do cartão"
                        }

                        is PaymentProcessingEvent.TRANSACTION_PROCESSING -> {
                            statusTextView.text = "Processando transação..."
                        }

                        is PaymentProcessingEvent.AUTHORIZING -> {
                            statusTextView.text = "Autorizando pagamento..."
                        }

                        is PaymentProcessingEvent.CARD_BIN_REQUESTED -> {
                            statusTextView.text = "Verificando cartão..."
                        }

                        is PaymentProcessingEvent.CARD_HOLDER_REQUESTED -> {
                            statusTextView.text = "Verificando titular..."
                        }

                        is PaymentProcessingEvent.CVV_REQUESTED -> {
                            statusTextView.text = "Verificando CVV..."
                        }

                        is PaymentProcessingEvent.DOWNLOADING_TABLES -> {
                            statusTextView.text = "Baixando tabelas..."
                        }

                        is PaymentProcessingEvent.CARD_REMOVAL_REQUESTING -> {
                            statusTextView.text = "Remova o cartão"
                        }

                        is PaymentProcessingEvent.CONTACTLESS_ON_DEVICE -> {
                            statusTextView.text = "Pagamento contactless detectado"
                        }

                        PaymentProcessingEvent.ACTIVATION_SUCCEEDED -> TODO()
                        PaymentProcessingEvent.APPROVAL_DECLINED -> TODO()
                        PaymentProcessingEvent.APPROVAL_SUCCEEDED -> TODO()
                        PaymentProcessingEvent.APPROVED_UPDATE_TRACK_3 -> TODO()
                        PaymentProcessingEvent.APPROVED_VIP -> TODO()
                        PaymentProcessingEvent.CANCELLED -> TODO()
                        PaymentProcessingEvent.CARD_BIN_OK -> TODO()
                        PaymentProcessingEvent.CARD_HOLDER_OK -> TODO()
                        PaymentProcessingEvent.CARD_REMOVAL_SUCCEEDED -> TODO()
                        PaymentProcessingEvent.CONTACTLESS_ERROR -> TODO()
                        PaymentProcessingEvent.CVV_OK -> TODO()
                        PaymentProcessingEvent.GENERIC_ERROR -> TODO()
                        PaymentProcessingEvent.GENERIC_SUCCESS -> TODO()
                        PaymentProcessingEvent.KEY_INSERTED -> TODO()
                        PaymentProcessingEvent.PARTIALLY_APPROVED -> TODO()
                        PaymentProcessingEvent.PIN_DIGIT_INPUT -> TODO()
                        PaymentProcessingEvent.PIN_DIGIT_REMOVED -> TODO()
                        PaymentProcessingEvent.PIN_OK -> TODO()
                        is PaymentProcessingEvent.QRCODE_SCAN -> TODO()
                        PaymentProcessingEvent.REQUEST_IN_PROGRESS -> TODO()
                        PaymentProcessingEvent.REVERSING_TRANSACTION_WITH_ERROR -> TODO()
                        PaymentProcessingEvent.SAVING_TABLES -> TODO()
                        PaymentProcessingEvent.SELECT_PAYMENT_METHOD -> TODO()
                        PaymentProcessingEvent.SOLVING_PENDING_ISSUES -> TODO()
                        PaymentProcessingEvent.START -> TODO()
                        PaymentProcessingEvent.SWITCH_INTERFACE -> TODO()
                        PaymentProcessingEvent.TRANSACTION_DONE -> TODO()
                    }
                }
            }
        }

        // Observer para eventos UI da ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.uiEvents.collect { event ->
                    paymentEventHandler.handleUiEvent(event)
                }
            }
        }

        // Observer para estados de pagamento (para controle geral)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.paymentState.collect { state ->
                    when (state) {
                        is PaymentState.Processing -> {
                            // Só atualiza se não tiver uma mensagem mais específica dos eventos
                            if (statusTextView.text.isNullOrEmpty() ||
                                statusTextView.text.contains("Aprovado") ||
                                statusTextView.text.contains("Erro") ||
                                statusTextView.text.contains("Cancelado")
                            ) {
                                statusTextView.text = "Processando pagamento..."
                            }
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
                            statusTextView.text = "Pronto para iniciar pagamento"
                        }

                        is PaymentState.Initializing -> {
                            statusTextView.text = "Inicializando sistema de pagamento..."
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.uiEvents.collect { event ->
                    paymentEventHandler.handleUiEvent(event)
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
        // Não sobreescreva mensagens específicas dos eventos
        val currentText = statusTextView.text.toString()
        if (currentText.isNullOrEmpty() ||
            !currentText.contains("cartão", ignoreCase = true) &&
            !currentText.contains("PIN", ignoreCase = true) &&
            !currentText.contains("processando", ignoreCase = true) &&
            !currentText.contains("verificando", ignoreCase = true)) {
            statusTextView.text = "Processando pagamento..."
        }
    }
    private fun updateUIForSuccess() {
        activity?.runOnUiThread {
            statusTextView.text = "Pagamento Aprovado!"
            // O PaymentEventHandler cuida da mensagem em infoTextView

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
            // O PaymentEventHandler já deve ter atualizado infoTextView com a mensagem de erro

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
                enqueuePayment(startImmediately = true) // Agora este método existe
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
                    isTransactionless = false
                )
                finishPaymentHandler.handlePayment(paymentTypeEnum, paymentData)
            } catch (e: Exception) {
                Log.e("CardPaymentFragment", "Erro ao processar pagamento finalizado: ${e.message}")
            }
        }
    }
}