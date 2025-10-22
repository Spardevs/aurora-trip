package br.com.ticpass.pos.view.fragments.payment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.repository.AcquisitionRepository
import br.com.ticpass.pos.data.room.service.PassGeneratorService
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.payment.PaymentState
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.payment.events.FinishPaymentHandler
import br.com.ticpass.pos.payment.events.PaymentType
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.printing.events.PrintingHandler
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.util.PaymentFragmentUtils
import br.com.ticpass.pos.view.fragments.printing.PrintingErrorDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingLoadingDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingSuccessDialogFragment
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
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
    private lateinit var titleTextView: TextView
    private var statusTextView: TextView? = null
    private var infoTextView: TextView? = null
    private lateinit var imageView: ImageView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvChangeValue: TextView
    private lateinit var etReceivedValue: EditText
    private var currentTransactionId: String? = null

    private var btnCancel: Button? = null
    private var btnConfirm: Button? = null
    private var totalAmount: BigDecimal = BigDecimal.ZERO
    private var amountReceived: BigDecimal = BigDecimal.ZERO
    private var loadingDialog: PrintingLoadingDialogFragment? = null
    private var successDialog: PrintingSuccessDialogFragment? = null
    private var errorDialog: PrintingErrorDialogFragment? = null
    private var observingPrintingState = false
    private var isPrintingInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AcquirerSdk.initialize(requireContext())

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
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvChangeValue = view.findViewById(R.id.tvChangeValue)
        etReceivedValue = view.findViewById(R.id.etReceivedValue)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnConfirm = view.findViewById(R.id.btnConfirm)

        etReceivedValue.requestFocus()

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etReceivedValue, InputMethodManager.SHOW_IMPLICIT)

        val buttonContainer = view.findViewById<View>(R.id.buttonContainer)

        statusTextView = view.findViewById(R.id.payment_status) ?: TextView(requireContext())
        infoTextView = view.findViewById(R.id.payment_info) ?: TextView(requireContext())

        passGeneratorService = PassGeneratorService(requireContext())
        setupPrintingHandler()
        setupObservers()
        observePrintingState()

        setupViews(view)
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            buttonContainer.animate().translationY(-imeHeight.toFloat()).setDuration(150).start()
            insets
        }

        tvTotalValue.text = formatCurrency(totalAmount.toBigInteger())
    }

    private fun setupViews(view: View) {
        val cart = shoppingCartManager.getCart()
        val totalCents: Long = try {
            cart.totalPrice.toLong()
        } catch (e: Exception) {
            cart.totalPrice.toString().toDoubleOrNull()?.toLong() ?: 0L
        }

        totalAmount = BigDecimal.valueOf(totalCents, 2).setScale(2, RoundingMode.HALF_EVEN)

        titleTextView.text = "Pagamento em Dinheiro"
        statusTextView?.text = "Aguardando confirmação"
        infoTextView?.text = "Informe o valor recebido em dinheiro (opcional)"

        btnConfirm?.isEnabled = true

        etReceivedValue.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val allowed = "0123456789.,"
            for (i in start until end) {
                if (!allowed.contains(source[i])) return@InputFilter ""
            }
            null
        })
    }

    private fun formatCurrencyFromReais(value: BigDecimal): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 2
        return nf.format(value)
    }

    private fun formatCurrency(valueInCents: BigInteger): String {
        val valueInReais = valueInCents.toDouble() / 100000.0
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

    private fun observePrintingState() {
        if (observingPrintingState) return
        observingPrintingState = true

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                printingViewModel.processingState.collect { state ->
                        Log.d("CashPaymentFragment", "ProcessingState: $state")
                    when (state) {
                        is ProcessingState.QueueDone<*> -> {
                            isPrintingInProgress = false
                            dismissLoadingModal()
                            showSuccessModal(autoDismissMs = 200L)
                        }

                        is ProcessingState.ItemFailed<*> -> {
                            isPrintingInProgress = false
                            dismissLoadingModal()
                            showErrorModal()
                        }

                        is ProcessingState.QueueAborted<*>,
                        is ProcessingState.QueueCanceled<*> -> {
                            isPrintingInProgress = false
                            dismissLoadingModal()
                        }

                        is ProcessingState.ItemDone<*> -> {
                            Timber.tag("Teste").d("${state.item}")
                            Timber.tag("Teste").d("${state.result}")
                            // Item concluído, mas aguardando fila completa
                        }

                        else -> {
                            // Outros estados
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        btnCancel?.setOnClickListener {
            finishNormally()
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
            tvChangeValue.text = formatCurrencyFromReais(BigDecimal.ZERO)
            etReceivedValue.error = null
            amountReceived = BigDecimal.ZERO
            return
        }

        val normalized = inputText.replace(',', '.')
        val parsed = try {
            BigDecimal(normalized).setScale(2, RoundingMode.HALF_EVEN)
        } catch (e: Exception) {
            null
        }

        if (parsed == null) {
            etReceivedValue.error = "Valor inválido"
            tvChangeValue.text = formatCurrencyFromReais(BigDecimal.ZERO)
            amountReceived = BigDecimal.ZERO
            return
        }

        amountReceived = parsed
        val change = amountReceived.subtract(totalAmount).setScale(2, RoundingMode.HALF_EVEN)

        tvChangeValue.visibility = View.VISIBLE
        tvChangeValue.text = formatCurrencyFromReais(change)
        val colorRes = if (change >= BigDecimal.ZERO) R.color.colorGreen else R.color.colorRed
        tvChangeValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        if (change < BigDecimal.ZERO) {
            etReceivedValue.error = getString(R.string.insufficient_amount)
        } else {
            etReceivedValue.error = null
        }
    }

    /**
     * Obtém o último transactionId (order) salvo no banco após finalizar o pagamento.
     */
    private suspend fun getLastTransactionIdOrNull(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val repo = AcquisitionRepository.getInstance(db.acquisitionDao())
                val last = repo.getLastAcquisition()
                Log.d("CashPaymentFragment", "last = $last")
                last?.order
            } catch (e: Exception) {
                Log.e("CashPaymentFragment", "Erro ao buscar transactionId: ${e.message}")
                null
            }
        }
    }

    /**
     * Obtém o valor de atk() do SDK ou fonte configurada.
     * AJUSTE CONFORME SUA IMPLEMENTAÇÃO REAL.
     */
    private fun getAtk(): String? {
        return try {
//             AcquirerSdk.atk()
            "1234567890"

        } catch (e: Exception) {
            Log.e("CashPaymentFragment", "Erro ao obter atk: ${e.message}")
            null
        }
    }

    private fun startPrintingProcessWithIds(atk: String?, transactionId: String?) {
        if (isPrintingInProgress) {
            Log.w("CashPaymentFragment", "Impressão já em andamento, ignorando nova tentativa")
            return
        }

        try {
            AcquirerSdk.initialize(requireContext())
            isPrintingInProgress = true

            // Limpa a fila antes de enfileirar novamente
            printingViewModel.cancelAllPrintings()

            val latestBitmap = getLatestPassBitmap()
            printingHandler.enqueueAndStartPrinting(
                printingViewModel = printingViewModel,
                imageBitmap = latestBitmap,
                atk = atk,
                transactionId = transactionId
            )
        } catch (e: Exception) {
            isPrintingInProgress = false
            Log.e("CashPaymentFragment", "Erro ao iniciar impressão: ${e.message}")
            updateUIForError("Erro ao iniciar impressão")
        }
    }

    private fun handleCashConfirmation() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoadingModal()
                paymentViewModel.notifyPaymentSuccess()
            } catch (e: Exception) {
                Log.e("CashPaymentFragment", "Erro ao processar pagamento em dinheiro: ${e.message}")
                updateUIForError("Erro ao processar pagamento")
                dismissLoadingModal()
            }
        }
    }

    private fun handleSuccessfulPayment() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoadingModal()
                val method = SystemPaymentMethod.CASH

                finishPaymentHandler.handlePayment(
                    PaymentType.SINGLE_PAYMENT,
                    PaymentUIUtils.PaymentData(
                        amount = (paymentValue * 1000.0).toInt(),
                        commission = 0,
                        method = method,
                        isTransactionless = true
                    )
                )

                // Obtém atk e transactionId após finalizar o pagamento
                val transactionId = currentTransactionId ?: getLastTransactionIdOrNull()
                val atk = getAtk()
                startPrintingProcessWithIds(atk, transactionId)
                Log.d("CashPaymentFragment", "atk=$atk, transactionId=$transactionId")

                // Monta o payload e o parse destruturado do barcode
                val payload = "${atk ?: ""}|${transactionId ?: ""}"
                val (parsedAtk, parsedTxId) = payload.split("|").let {
                    val a = it.getOrNull(0)?.takeIf { s -> s.isNotEmpty() } ?: "(atk não disponível)"
                    val t = it.getOrNull(1)?.takeIf { s -> s.isNotEmpty() } ?: "(transactionId não disponível)"
                    Pair(a, t)
                }

                startPrintingProcessWithIds(atk, transactionId)

            } catch (e: Exception) {
                dismissLoadingModal()
                updateUIForError("Erro ao finalizar pagamento")
            }
        }
    }

    private fun updateUIForSuccess() {
        activity?.runOnUiThread {
            statusTextView?.text = "Pagamento Confirmado!"
            val inputText = etReceivedValue.text.toString()
            infoTextView?.text = if (inputText.isNotEmpty()) {
                val troco = amountReceived.subtract(totalAmount)
                "Fichas geradas com sucesso. Troco: ${formatCurrencyFromReais(troco)}"
            } else {
                "Fichas geradas com sucesso. Valor não informado."
            }

            imageView.setImageResource(R.drawable.ic_check)

            btnCancel?.text = "Finalizar"
            btnCancel?.isEnabled = true
            btnCancel?.setOnClickListener {
                finishNormally()
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
                finishNormally()
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
                finishNormally()
            }
        }
    }

    private fun updateUIForCancelled() {
        statusTextView?.text = "Pagamento Cancelado"
        infoTextView?.text = "A transação foi cancelada"
    }

    private fun showLoadingModal() {
        if (loadingDialog?.isAdded == true) return
        loadingDialog = PrintingLoadingDialogFragment()
        loadingDialog?.show(parentFragmentManager, "printing_loading")
    }

    private fun dismissLoadingModal() {
        loadingDialog?.dismissAllowingStateLoss()
        loadingDialog = null
    }

    private fun showSuccessModal(autoDismissMs: Long = 800L) {
        successDialog = PrintingSuccessDialogFragment().apply {
            onFinishListener = object : PrintingSuccessDialogFragment.OnFinishListener {
                override fun onFinish() {
                    finishNormally()
                }
            }
        }
        successDialog?.show(parentFragmentManager, "printing_success")
        successDialog?.dialog?.window?.decorView?.postDelayed({
            successDialog?.onFinishListener?.onFinish()
        }, autoDismissMs)
    }

    private fun showErrorModal() {
        errorDialog = PrintingErrorDialogFragment().apply {
            cancelPrintingListener = object : PrintingErrorDialogFragment.OnCancelPrintingListener {
                override fun onCancelPrinting() {
                    printingViewModel.cancelAllPrintings()
                    isPrintingInProgress = false
                    finishNormally()
                }
            }
            retryPrintingListener = object : PrintingErrorDialogFragment.OnRetryPrintingListener {
                override fun onRetryPrinting() {
                    dismissLoadingModal()
                    errorDialog?.dismissAllowingStateLoss()

                    viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.delay(200)
                        showLoadingModal()

                        val transactionId = getLastTransactionIdOrNull()
                        val atk = getAtk()
                        startPrintingProcessWithIds(atk, transactionId)
                    }
                }
            }
        }
        errorDialog?.show(parentFragmentManager, "printing_error")
    }

    private fun finishNormally() {
        try {
            dismissLoadingModal()
            successDialog?.dismissAllowingStateLoss()
            errorDialog?.dismissAllowingStateLoss()
        } catch (_: Exception) {}
        shoppingCartManager.clearCart()
        requireActivity().setResult(AppCompatActivity.RESULT_OK)
        requireActivity().finish()
    }

    private fun getLatestPassBitmap(): Bitmap? {
        return try {
            val dir = File(requireContext().cacheDir, "printing")
            if (!dir.exists()) return null
            val files = dir.listFiles()?.filter { it.isFile } ?: return null
            val latest = files.maxByOrNull { it.lastModified() } ?: return null
            BitmapFactory.decodeFile(latest.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}