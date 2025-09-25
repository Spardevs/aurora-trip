package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.service.PassGeneratorService
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.payment.PaymentState
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.payment.events.FinishPaymentHandler
import br.com.ticpass.pos.payment.events.PaymentType
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.TimeoutCountdownView
import br.com.ticpass.pos.printing.events.PrintingHandler
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.util.PaymentFragmentUtils
import br.com.ticpass.pos.view.ui.pass.PassType
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CashPaymentFragment : Fragment() {

    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()
    private val printingViewModel: PrintingViewModel by activityViewModels()

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    @Inject
    lateinit var finishPaymentHandler: FinishPaymentHandler

    @Inject
    lateinit var paymentUtils: PaymentFragmentUtils

    private var paymentValue: Double = 0.0
    private var totalValue: Double = 0.0
    private var remainingValue: Double = 0.0
    private var isMultiPayment: Boolean = false
    private var progress: String = ""

    private lateinit var passGeneratorService: PassGeneratorService
     private lateinit var printingHandler: PrintingHandler

    private lateinit var timeoutCountdownView: TimeoutCountdownView
    private lateinit var titleTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var cancelButton: MaterialButton
    private lateinit var confirmButton: MaterialButton
    private lateinit var amountReceivedEditText: TextInputEditText
    private lateinit var amountReceivedLayout: TextInputLayout
    private lateinit var changeContainer: LinearLayout
    private lateinit var changeAmountTextView: TextView

    private var totalAmount: Double = 0.0
    private var amountReceived: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(requireContext())
        super.onCreate(savedInstanceState)

        arguments?.let {
            paymentValue = it.getDouble("value_to_pay", 0.0)
            totalValue = it.getDouble("total_value", paymentValue)
            remainingValue = it.getDouble("remaining_value", paymentValue)
            isMultiPayment = it.getBoolean("is_multi_payment", false)
            progress = it.getString("progress", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_cash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passGeneratorService = PassGeneratorService(requireContext())
        setupViews(view)
        setupPrintingHandler()
        setupObservers()
        setupListeners()
    }

    private fun setupViews(view: View) {
        titleTextView = view.findViewById(R.id.payment_form)
        statusTextView = view.findViewById(R.id.payment_status)
        infoTextView = view.findViewById(R.id.payment_info)
        imageView = view.findViewById(R.id.image)
        priceTextView = view.findViewById(R.id.payment_price)
        cancelButton = view.findViewById(R.id.btn_cancel)
        confirmButton = view.findViewById(R.id.btn_confirm)
        amountReceivedEditText = view.findViewById(R.id.amountReceivedEditText)
        amountReceivedLayout = view.findViewById(R.id.amountReceivedLayout)
        changeContainer = view.findViewById(R.id.changeContainer)
        changeAmountTextView = view.findViewById(R.id.changeAmountTextView)
        timeoutCountdownView = view.findViewById(R.id.timeoutCountdownView)

        totalAmount = paymentValue
        priceTextView.text = PaymentFragmentUtils.formatCurrency(paymentValue)

        if (isMultiPayment && progress.isNotEmpty()) {
            val progressTextView = view.findViewById<TextView>(R.id.tv_progress)
            progressTextView?.visibility = View.VISIBLE
            progressTextView?.text = "Pagamento $progress"
        }

        val cart = shoppingCartManager.getCart()
        totalAmount = cart.totalPrice.toDouble() / 100.0

        titleTextView.text = "Pagamento em Dinheiro"
        statusTextView.text = "Aguardando confirmação"
        infoTextView.text = "Informe o valor recebido em dinheiro (opcional)"
        priceTextView.text = PaymentFragmentUtils.formatCurrency(totalAmount)

        confirmButton.isEnabled = true
        amountReceivedLayout.hint = "Valor recebido (opcional)"
    }

    private fun setupPrintingHandler() {
        printingHandler = PrintingHandler(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner
        )
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.paymentState.collect { state ->
                    when (state) {
                        is PaymentState.Success -> handleSuccessfulPayment()
                        is PaymentState.Error -> updateUIForError(state.errorMessage)
                        is PaymentState.Cancelled -> updateUIForCancelled()
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        cancelButton.setOnClickListener {
            requireActivity().finish()
        }

        confirmButton.setOnClickListener {
            handleCashConfirmation()
        }

        amountReceivedEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                calculateChange()
            }
        }

        amountReceivedEditText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                calculateChange()
                true
            } else {
                false
            }
        }

        amountReceivedEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateChange()
            }
        })
    }

    private fun calculateChange() {
        try {
            val inputText = amountReceivedEditText.text.toString()
            if (inputText.isNotEmpty()) {
                amountReceived = inputText.toDouble()
                val change = amountReceived - totalAmount

                if (change >= 0) {
                    changeContainer.visibility = View.VISIBLE
                    changeAmountTextView.text = PaymentFragmentUtils.formatCurrency(change)
                    changeAmountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen))
                    amountReceivedLayout.error = null
                } else {
                    changeContainer.visibility = View.VISIBLE
                    changeAmountTextView.text = PaymentFragmentUtils.formatCurrency(change)
                    changeAmountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorRed))
                    amountReceivedLayout.error = getString(R.string.insufficient_amount)
                }
            } else {
                changeContainer.visibility = View.GONE
                amountReceivedLayout.error = null
                amountReceived = 0.0
            }
        } catch (e: NumberFormatException) {
            changeContainer.visibility = View.GONE
            amountReceivedLayout.error = "Valor inválido"
            amountReceived = 0.0
        }
    }

    private fun handleCashConfirmation() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val inputText = amountReceivedEditText.text.toString()
                amountReceived = if (inputText.isNotEmpty()) inputText.toDouble() else 0.0

                statusTextView.text = "Processando..."
                infoTextView.text = when {
                    amountReceived == 0.0 -> "Confirmando pagamento sem valor informado"
                    amountReceived >= totalAmount -> "Processando pagamento com troco"
                    else -> "Confirmando pagamento com valor parcial"
                }

                amountReceivedEditText.isEnabled = false
                confirmButton.isEnabled = false
                cancelButton.isEnabled = false

                paymentViewModel.notifyPaymentSuccess()

                // Iniciar impressão via PrintingHandler
                val composeView = ComposeView(requireContext())
                printingHandler.generateTickets(
                    composeView = composeView,
                    passType = PassType.ProductCompact,
                    printingViewModel = printingViewModel
                )

                requireActivity().finish()

            } catch (e: Exception) {
                Log.e("CashPaymentFragment", "Erro ao processar pagamento em dinheiro: ${e.message}")
                updateUIForError("Erro ao processar pagamento")

                amountReceivedEditText.isEnabled = true
                confirmButton.isEnabled = true
                cancelButton.isEnabled = true
            }
        }
    }

    private fun updateUIForSuccess() {
        activity?.runOnUiThread {
            statusTextView.text = "Pagamento Confirmado!"
            val inputText = amountReceivedEditText.text.toString()
            infoTextView.text = if (inputText.isNotEmpty()) {
                "Fichas geradas com sucesso. Troco: ${PaymentFragmentUtils.formatCurrency(amountReceived - totalAmount)}"
            } else {
                "Fichas geradas com sucesso. Valor não informado."
            }

            imageView.setImageResource(R.drawable.ic_check)

            cancelButton.text = "Finalizar"
            cancelButton.isEnabled = true
            cancelButton.setOnClickListener {
                shoppingCartManager.clearCart()
                requireActivity().finish()
            }

            confirmButton.visibility = View.GONE
            amountReceivedLayout.visibility = View.GONE
            changeContainer.visibility = View.GONE
        }
    }

    private fun updateUIForError(errorMessage: String) {
        activity?.runOnUiThread {
            statusTextView.text = "Erro no Processamento"
            infoTextView.text = errorMessage
            imageView.setImageResource(R.drawable.ic_close)

            cancelButton.isEnabled = true
            cancelButton.text = "Cancelar"
            cancelButton.setOnClickListener {
                requireActivity().finish()
            }

            confirmButton.visibility = View.VISIBLE
            confirmButton.text = "Tentar Novamente"
            confirmButton.setOnClickListener {
                retryPayment()
            }
        }
    }

    private fun retryPayment() {
        activity?.runOnUiThread {
            confirmButton.visibility = View.VISIBLE
            confirmButton.text = "Confirmar Recebimento"
            confirmButton.isEnabled = true
            statusTextView.text = "Aguardando confirmação"
            infoTextView.text = "Informe o valor recebido em dinheiro (opcional)"
            imageView.setImageResource(R.drawable.ic_credit_card)

            amountReceivedEditText.text?.clear()
            amountReceivedEditText.isEnabled = true
            changeContainer.visibility = View.GONE
            amountReceivedLayout.error = null

            cancelButton.isEnabled = true
            cancelButton.text = "Cancelar"
            cancelButton.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    private fun updateUIForCancelled() {
        statusTextView.text = "Pagamento Cancelado"
        infoTextView.text = "A transação foi cancelada"
    }

    private fun handleSuccessfulPayment() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val method = SystemPaymentMethod.CASH

                finishPaymentHandler.handlePayment(
                    PaymentType.SINGLE_PAYMENT,
                    PaymentUIUtils.PaymentData(
                        amount = (paymentValue * 100).toInt(),
                        commission = 0,
                        method = method,
                        isTransactionless = true
                    )
                )

                // Imprimir fichas
                val composeView = ComposeView(requireContext())
                printingHandler.generateTickets(
                    composeView = composeView,
                    passType = PassType.ProductCompact,
                    printingViewModel = printingViewModel
                )

                if (isMultiPayment) {
                    navigateBackToSelection()
                } else {
                    shoppingCartManager.clearCart()
                    requireActivity().setResult(AppCompatActivity.RESULT_OK)
                    requireActivity().finish()
                }

            } catch (e: Exception) {
                updateUIForError("Erro ao finalizar pagamento")
            }
        }
    }

    private fun navigateBackToSelection() {
        val newRemainingValue = remainingValue - paymentValue

        if (newRemainingValue > 0) {
            requireActivity().setResult(AppCompatActivity.RESULT_OK)
            requireActivity().finish()
        } else {
            shoppingCartManager.clearCart()
            requireActivity().setResult(AppCompatActivity.RESULT_OK)
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
}