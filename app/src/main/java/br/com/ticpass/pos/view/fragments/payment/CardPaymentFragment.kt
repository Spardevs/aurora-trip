package br.com.ticpass.pos.view.fragments.payment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.PaymentSelectionActivity
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.events.FinishPaymentHandler
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.events.PaymentType
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.TimeoutCountdownView
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.util.PaymentFragmentUtils
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardPaymentFragment : Fragment() {
    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()
    private lateinit var paymentEventHandler: PaymentEventHandler

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    @Inject
    lateinit var finishPaymentHandler: FinishPaymentHandler

    @Inject
    lateinit var paymentUtils: PaymentFragmentUtils
    private var shouldStartImmediately = false

    private lateinit var titleTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var cancelButton: MaterialButton
    private lateinit var retryButton: MaterialButton
    private lateinit var timeoutCountdownView: TimeoutCountdownView

    private var paymentType: String? = null
    private var paymentValue: Double = 0.0
    private var totalValue: Double = 0.0
    private var remainingValue: Double = 0.0
    private var isMultiPayment: Boolean = false
    private var progress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(requireContext())
        super.onCreate(savedInstanceState)

        paymentType = arguments?.getString("payment_type")
        paymentValue = arguments?.getDouble("value_to_pay") ?: 0.0
        totalValue = arguments?.getDouble("total_value") ?: paymentValue
        remainingValue = arguments?.getDouble("remaining_value") ?: paymentValue
        isMultiPayment = arguments?.getBoolean("is_multi_payment") ?: false
        progress = arguments?.getString("progress") ?: ""

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
            enqueuePayment(startImmediately = true)
        }
    }

    private fun setupPaymentEventHandler(view: View) {
        timeoutCountdownView = view.findViewById(R.id.timeout_countdown)

        paymentEventHandler = PaymentEventHandler(
            context = requireContext(),
            dialogEventTextView = infoTextView,
            dialogQRCodeImageView = imageView,
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
        priceTextView.text = PaymentFragmentUtils.formatCurrency(paymentValue)

        if (isMultiPayment && progress.isNotEmpty()) {
            val progressTextView = view.findViewById<TextView>(R.id.tv_progress)
            progressTextView?.visibility = View.VISIBLE
            progressTextView?.text = "Pagamento $progress"
        }

        // Usar o valor específico deste pagamento, não o total do carrinho
        priceTextView.text = PaymentFragmentUtils.formatCurrency(paymentValue)

        when (paymentType) {
            "credit_card" -> {
                titleTextView.text = "Cartão de Crédito"
                statusTextView.text = "Aguardando pagamento no crédito"
                infoTextView.text = "Aproxime, insira ou passe o cartão de crédito"
                imageView.setImageResource(R.drawable.ic_credit_card)
            }
            "debit_card" -> {
                titleTextView.text = "Cartão de Débito"
                statusTextView.text = "Aguardando pagamento no débito"
                infoTextView.text = "Aproxime, insira ou passe o cartão de débito"
                imageView.setImageResource(R.drawable.ic_credit_card)
            }
        }

        cancelButton.setOnClickListener {
            paymentViewModel.abortAllPayments()
            requireActivity().finish()
        }
    }

    private fun setupObservers() {
        PaymentFragmentUtils.setupPaymentObservers(
            fragment = this,
            paymentViewModel = paymentViewModel,
            paymentEventHandler = paymentEventHandler,
            statusTextView = statusTextView,
            onSuccess = { handleSuccessfulPayment() },
            onError = { errorMessage -> updateUIForError(errorMessage) },
            onCancelled = { updateUIForCancelled() }
        )
    }

    private fun enqueuePayment(startImmediately: Boolean) {
        val method = when (paymentType) {
            "credit_card" -> SystemPaymentMethod.CREDIT
            "debit_card" -> SystemPaymentMethod.DEBIT
            else -> return
        }

        PaymentFragmentUtils.enqueuePayment(
            paymentViewModel = paymentViewModel,
            shoppingCartManager = shoppingCartManager,
            method = method,
            amount = paymentValue, // Passar o valor específico
            isTransactionless = true,
            startImmediately = startImmediately
        )
    }

    private fun updateUIForSuccess() {
        activity?.runOnUiThread {
            statusTextView.text = "Pagamento Aprovado!"
            imageView.setImageResource(R.drawable.ic_check)

            cancelButton.text = if (isMultiPayment) "Próximo Pagamento" else "Finalizar"
            cancelButton.setOnClickListener {
                if (isMultiPayment) {
                    requireActivity().setResult(AppCompatActivity.RESULT_OK)
                    requireActivity().finish()
                } else {
                    shoppingCartManager.clearCart()
                    requireActivity().finish()
                }
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

    private fun navigateBackToSelection() {
        val newRemainingValue = remainingValue - paymentValue
        if (newRemainingValue > 0) {
            // Ainda há valor a ser pago, voltar para seleção
            val intent = Intent(requireContext(), PaymentSelectionActivity::class.java).apply {
                putExtra("total_value", totalValue)
                putExtra("remaining_value", newRemainingValue)
                putExtra("is_multi_payment", true)
                putExtra("progress", getNextProgress(progress))
            }
            startActivity(intent)
            requireActivity().finish()
        } else {
            // Todos os pagamentos foram concluídos
            shoppingCartManager.clearCart()
            requireActivity().finish()
        }
    }


    private fun getNextProgress(currentProgress: String): String {
        return try {
            val parts = currentProgress.split("/")
            if (parts.size == 2) {
                val current = parts[0].toInt()
                val total = parts[1].toInt()
                "${current + 1}/$total"
            } else {
                "2/?"
            }
        } catch (e: Exception) {
            "2/?"
        }
    }

    private fun updateUIForError(errorMessage: String) {
        activity?.runOnUiThread {
            statusTextView.text = "Erro no Pagamento"
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

    private fun handleSuccessfulPayment() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val method = when (paymentType) {
                    "credit_card" -> SystemPaymentMethod.CREDIT
                    "debit_card" -> SystemPaymentMethod.DEBIT
                    else -> SystemPaymentMethod.CREDIT
                }

                val paymentData = PaymentUIUtils.PaymentData(
                    amount = (paymentValue * 100).toInt(), // Converter para centavos
                    commission = 0,
                    method = method,
                    isTransactionless = true
                )

                finishPaymentHandler.handlePayment(
                    PaymentType.SINGLE_PAYMENT,
                    PaymentUIUtils.PaymentData(
                        amount = (paymentValue * 100).toInt(),
                        commission = 0,
                        method = when (paymentType) {
                            "credit_card" -> SystemPaymentMethod.CREDIT
                            "debit_card" -> SystemPaymentMethod.DEBIT
                            "pix" -> SystemPaymentMethod.PIX
                            else -> SystemPaymentMethod.CREDIT
                        },
                        isTransactionless = true
                    )
                )

                requireActivity().setResult(AppCompatActivity.RESULT_OK)
                requireActivity().finish()
            } catch (e: Exception) {
                updateUIForError("Erro ao finalizar pagamento")
            }
        }
    }
}