package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale
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
    // Views mapeadas ao layout que você enviou
    private lateinit var titleTextView: TextView
    private var statusTextView: TextView? = null
    private var infoTextView: TextView? = null
    private lateinit var imageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvChangeValue: TextView
    private lateinit var etReceivedValue: EditText
    private var btnCancel: Button? = null
    private var btnConfirm: Button? = null
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
            progress = it.getString("progress", "") ?: ""
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

        titleTextView = view.findViewById(R.id.tvMoneyTitle)
        imageView = view.findViewById(R.id.iconMoney)
        priceTextView = view.findViewById(R.id.tvTotalValue)

        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvChangeValue = view.findViewById(R.id.tvChangeValue)
        etReceivedValue = view.findViewById(R.id.etReceivedValue)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnConfirm = view.findViewById(R.id.btnConfirm)

        statusTextView = view.findViewById(R.id.payment_status) ?: TextView(requireContext())
        infoTextView = view.findViewById(R.id.payment_info) ?: TextView(requireContext())

        passGeneratorService = PassGeneratorService(requireContext())
        setupPrintingHandler()
        setupObservers()

        setupViews(view)

        setupListeners()

        tvTotalValue.text = PaymentFragmentUtils.formatCurrency(totalAmount)
        priceTextView.text = PaymentFragmentUtils.formatCurrency(totalAmount)
    }

    private fun setupViews(view: View) {
        val cart = shoppingCartManager.getCart()
        totalAmount = cart.totalPrice.toDouble() / 10000.0

        titleTextView.text = "Pagamento em Dinheiro"
        statusTextView?.text = "Aguardando confirmação"
        infoTextView?.text = "Informe o valor recebido em dinheiro (opcional)"
        priceTextView.text = formatCurrency(cart.totalPrice)

        btnConfirm?.isEnabled = true
    }

    private fun formatCurrency(valueInCents: BigInteger): String {
        val valueInReais = valueInCents.toDouble() / 100.0
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(valueInReais)
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
        btnCancel?.setOnClickListener {
            requireActivity().finish()
        }

        btnConfirm?.setOnClickListener {
            handleCashConfirmation()
        }

        etReceivedValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                calculateChange()
            }
        }

        etReceivedValue.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                calculateChange()
                true
            } else {
                false
            }
        }

        etReceivedValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateChange()
            }
        })
    }

    private fun calculateChange() {
        val inputText = etReceivedValue.text.toString().trim()
        if (inputText.isEmpty()) {
            tvChangeValue.visibility = View.VISIBLE
            tvChangeValue.text = PaymentFragmentUtils.formatCurrency(0.0)
            etReceivedValue.error = null
            amountReceived = 0.0
            return
        }

        val parsed = inputText.replace(',', '.').toDoubleOrNull()
        if (parsed == null) {
            etReceivedValue.error = "Valor inválido"
            tvChangeValue.text = PaymentFragmentUtils.formatCurrency(0.0)
            amountReceived = 0.0
            return
        }

        amountReceived = parsed
        val change = amountReceived - totalAmount

        tvChangeValue.visibility = View.VISIBLE
        tvChangeValue.text = PaymentFragmentUtils.formatCurrency(change)
        val colorRes = if (change >= 0) R.color.colorGreen else R.color.colorRed
        tvChangeValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        if (change < 0) {
            etReceivedValue.error = getString(R.string.insufficient_amount)
        } else {
            etReceivedValue.error = null
        }
    }

    private fun handleCashConfirmation() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val inputText = etReceivedValue.text.toString()
                amountReceived = if (inputText.isNotEmpty()) inputText.replace(',', '.').toDouble() else 0.0

                statusTextView?.text = "Processando..."
                infoTextView?.text = when {
                    amountReceived == 0.0 -> "Confirmando pagamento sem valor informado"
                    amountReceived >= totalAmount -> "Processando pagamento com troco"
                    else -> "Confirmando pagamento com valor parcial"
                }

                etReceivedValue.isEnabled = false
                btnConfirm?.isEnabled = false
                btnCancel?.isEnabled = false

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

                etReceivedValue.isEnabled = true
                btnConfirm?.isEnabled = true
                btnCancel?.isEnabled = true
            }
        }
    }

    private fun updateUIForSuccess() {
        activity?.runOnUiThread {
            statusTextView?.text = "Pagamento Confirmado!"
            val inputText = etReceivedValue.text.toString()
            infoTextView?.text = if (inputText.isNotEmpty()) {
                "Fichas geradas com sucesso. Troco: ${PaymentFragmentUtils.formatCurrency(amountReceived - totalAmount)}"
            } else {
                "Fichas geradas com sucesso. Valor não informado."
            }

            imageView.setImageResource(R.drawable.ic_check)

            btnCancel?.text = "Finalizar"
            btnCancel?.isEnabled = true
            btnCancel?.setOnClickListener {
                shoppingCartManager.clearCart()
                requireActivity().finish()
            }

            btnConfirm?.visibility = View.GONE
            etReceivedValue.visibility = View.GONE
            tvChangeValue.visibility = View.GONE
        }
    }

    private fun updateUIForError(errorMessage: String) {
        activity?.runOnUiThread {
            statusTextView?.text = "Erro no Processamento"
            infoTextView?.text = errorMessage
            imageView.setImageResource(R.drawable.ic_close)

            btnCancel?.isEnabled = true
            btnCancel?.text = "Cancelar"
            btnCancel?.setOnClickListener {
                requireActivity().finish()
            }

            btnConfirm?.visibility = View.VISIBLE
            btnConfirm?.text = "Tentar Novamente"
            btnConfirm?.setOnClickListener {
                retryPayment()
            }
        }
    }

    private fun retryPayment() {
        activity?.runOnUiThread {
            btnConfirm?.visibility = View.VISIBLE
            btnConfirm?.text = "Confirmar Recebimento"
            btnConfirm?.isEnabled = true
            statusTextView?.text = "Aguardando confirmação"
            infoTextView?.text = "Informe o valor recebido em dinheiro (opcional)"
            imageView.setImageResource(R.drawable.ic_credit_card)

            etReceivedValue.text?.clear()
            etReceivedValue.isEnabled = true
            tvChangeValue.visibility = View.GONE
            etReceivedValue.error = null

            btnCancel?.isEnabled = true
            btnCancel?.text = "Cancelar"
            btnCancel?.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    private fun updateUIForCancelled() {
        statusTextView?.text = "Pagamento Cancelado"
        infoTextView?.text = "A transação foi cancelada"
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