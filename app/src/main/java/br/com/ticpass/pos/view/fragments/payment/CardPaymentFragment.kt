package br.com.ticpass.pos.view.fragments.payment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.payment.events.FinishPaymentHandler
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.events.PaymentType
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.payment.view.TimeoutCountdownView
import br.com.ticpass.pos.printing.events.PrintingHandler
import br.com.ticpass.pos.queue.models.PaymentSuccess
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.util.PaymentFragmentUtils
import br.com.ticpass.pos.view.fragments.printing.PrintingErrorDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingLoadingDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingSuccessDialogFragment
import br.com.ticpass.pos.view.ui.pass.PassType
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CardPaymentFragment : Fragment() {
    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()
    private val printingViewModel: PrintingViewModel by activityViewModels()

    private lateinit var paymentEventHandler: PaymentEventHandler
    private lateinit var printingHandler: PrintingHandler

    @Inject lateinit var shoppingCartManager: ShoppingCartManager
    @Inject lateinit var finishPaymentHandler: FinishPaymentHandler
    @Inject lateinit var paymentUtils: PaymentFragmentUtils

    private lateinit var titleTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var cancelButton: MaterialButton
    private lateinit var retryButton: MaterialButton
    private lateinit var timeoutCountdownView: TimeoutCountdownView
    private lateinit var pinInputLayout: TextInputLayout
    private lateinit var pinInput: TextInputEditText
    private lateinit var submitPinButton: MaterialButton
    private var paymentType: String? = null
    private var paymentValue = 0.0
    private var totalValue = 0.0
    private var remainingValue = 0.0
    private var isMultiPayment = false
    private var progress = ""

    private var lastTxId: String? = null
    private var lastAtk: String? = null

    private var finishPaymentHandled = false
    private var printingStarted = false

    private var loadingDialog: PrintingLoadingDialogFragment? = null
    private var successDialog: PrintingSuccessDialogFragment? = null
    private var errorDialog: PrintingErrorDialogFragment? = null
    private var observingPrintingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(requireContext())
        super.onCreate(savedInstanceState)
        loadPaymentData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_payment_card, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::shoppingCartManager.isInitialized) {
            Log.e(TAG, "ShoppingCartManager not initialized")
            requireActivity().finish()
            return
        }

        bindViews(view)
        setupHandlers()
        setupObservers()
        observePaymentProcessingState()

        setFragmentResultListener("retry_payment") { _, _ -> retryPayment() }
    }

    override fun onResume() {
        super.onResume()
        if (::shoppingCartManager.isInitialized) enqueueAndStartPayment()
    }

    private fun loadPaymentData() {
        val sharedPrefs = requireContext().getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
        val shoppingCartDataJson = sharedPrefs.getString("shopping_cart_data", null)

        if (shoppingCartDataJson != null) {
            try {
                val jsonObject = JSONObject(shoppingCartDataJson)
                val totalPriceCents = jsonObject.optLong("totalPrice", 0L)
                val totalPriceReais = totalPriceCents / 10000.0
                totalValue = totalPriceReais
                paymentValue = totalPriceReais
                remainingValue = totalPriceReais
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao parsear totalPrice do SharedPreferences", e)
            }
        } else {
            paymentType = arguments?.getString("payment_type")
            paymentValue = arguments?.getDouble("value_to_pay") ?: 0.0
            totalValue = arguments?.getDouble("total_value") ?: paymentValue
            remainingValue = arguments?.getDouble("remaining_value") ?: paymentValue
        }

        isMultiPayment = arguments?.getBoolean("is_multi_payment") ?: false
        progress = arguments?.getString("progress") ?: ""
        if (paymentType.isNullOrBlank()) paymentType = requireActivity().intent.getStringExtra("payment_type")
        paymentType = paymentType?.lowercase().takeIf { it == "credit_card" || it == "debit_card" } ?: "credit_card"
    }

    private fun bindViews(view: View) {
        titleTextView = view.findViewById(R.id.payment_form)
        statusTextView = view.findViewById(R.id.payment_status)
        infoTextView = view.findViewById(R.id.payment_info)
        imageView = view.findViewById(R.id.image)
        priceTextView = view.findViewById(R.id.payment_price)
        cancelButton = view.findViewById(R.id.btn_cancel)
        retryButton = view.findViewById(R.id.btn_retry)
        timeoutCountdownView = view.findViewById(R.id.timeout_countdown)
        pinInputLayout = view.findViewById(R.id.pin_input_layout)
        pinInput = view.findViewById(R.id.pin_input)
        submitPinButton = view.findViewById(R.id.btn_submit_pin)

        priceTextView.text = PaymentFragmentUtils.formatCurrency(paymentValue)

        if (isMultiPayment) view.findViewById<TextView>(R.id.tv_progress)?.apply {
            visibility = View.VISIBLE
            text = "Pagamento $progress"
        }

        when (paymentType) {
            "credit_card" -> {
                titleTextView.text = "Cartão de Crédito"
                statusTextView.text = "Aguardando pagamento no crédito"
                infoTextView.text = "Aproxime, insira ou passe o cartão de crédito"
            }
            "debit_card" -> {
                titleTextView.text = "Cartão de Débito"
                statusTextView.text = "Aguardando pagamento no débito"
                infoTextView.text = "Aproxime, insira ou passe o cartão de débito"
            }
        }
        imageView.setImageResource(R.drawable.ic_credit_card)
        cancelButton.setOnClickListener {
            paymentViewModel.abortAllPayments()
            requireActivity().finish()
        }
        retryButton.visibility = View.GONE
    }

    private fun setupHandlers() {
        paymentEventHandler = PaymentEventHandler(requireContext(), infoTextView, imageView, timeoutCountdownView)
        printingHandler = PrintingHandler(requireContext(), viewLifecycleOwner)
    }

    private fun setupObservers() {
        PaymentFragmentUtils.setupPaymentObservers(
            fragment = this,
            paymentViewModel = paymentViewModel,
            paymentEventHandler = paymentEventHandler,
            statusTextView = statusTextView,
            onSuccess = { handleSuccessfulPayment() },
            onError = { errorMessage -> showErrorFragment(errorMessage) },
            onCancelled = { showErrorFragment("Pagamento cancelado") },
            isPix = false,
            onProcessingItemDone = { paymentSuccess, _ ->
                handlePaymentSuccessData(paymentSuccess.txId, paymentSuccess.atk)
            }
        )
    }

    private fun observePaymentProcessingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.processingState.collect { state ->
                    if (state is ProcessingState.ItemDone<*> && state.result is PaymentSuccess) {
                        val paymentSuccess = state.result as PaymentSuccess
                        handlePaymentSuccessData(paymentSuccess.txId, paymentSuccess.atk)
                    }
                }
            }
        }
    }

    private fun handlePaymentSuccessData(txId: String, atk: String) {
        lastTxId = txId
        lastAtk = atk
        Log.d("PaymentSuccess", "txId: $txId, atk: $atk")
        triggerPrintingIfReady()
    }

    private fun enqueueAndStartPayment() {
        val method = when (paymentType) {
            "credit_card" -> SystemPaymentMethod.CREDIT
            "debit_card" -> SystemPaymentMethod.DEBIT
            else -> {
                showErrorFragment("Tipo de pagamento inválido")
                return
            }
        }

        PaymentFragmentUtils.enqueuePayment(
            paymentViewModel = paymentViewModel,
            shoppingCartManager = shoppingCartManager,
            method = method,
            amount = (paymentValue * 100),
            isTransactionless = false,
            startImmediately = true
        )

        try {
            paymentViewModel.startProcessing()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar o processamento de pagamentos: ${e.message}", e)
        }
    }

    private fun retryPayment() {
        finishPaymentHandled = false
        printingStarted = false
        lastTxId = null
        lastAtk = null

        activity?.runOnUiThread {
            retryButton.visibility = View.GONE
            statusTextView.text = "Preparando nova tentativa..."
            infoTextView.text = "Aguarde..."
            imageView.setImageResource(R.drawable.ic_credit_card)
            imageView.clearColorFilter()

            Handler(Looper.getMainLooper()).postDelayed({
                enqueueAndStartPayment()
            }, 1000)
        }
    }

    private fun handleSuccessfulPayment() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoadingModal()

                val method = when (paymentType) {
                    "credit_card" -> SystemPaymentMethod.CREDIT
                    "debit_card" -> SystemPaymentMethod.DEBIT
                    else -> SystemPaymentMethod.CREDIT
                }

                val amountInCents = (paymentValue * 100).toInt()

                finishPaymentHandler.handlePayment(
                    PaymentType.SINGLE_PAYMENT,
                    PaymentUIUtils.PaymentData(
                        amount = amountInCents,
                        commission = 0,
                        method = method,
                        isTransactionless = false
                    )
                )

                // marque que o finishPayment foi tratado e tente disparar impressão
                finishPaymentHandled = true
                triggerPrintingIfReady()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao finalizar pagamento", e)
                dismissLoadingModal()
                showErrorFragment("Erro ao finalizar pagamento")
                // reset flags em caso de erro
                finishPaymentHandled = false
                printingStarted = false
            }
        }
    }

    private fun startPrintingProcess(transactionId: String? = null, atk: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AcquirerSdk.initialize(requireContext())

                val transactionIdToUse = transactionId ?: lastTxId
                val atkToUse = atk ?: lastAtk

                Log.d(TAG, "startPrintingProcess: tx=$transactionIdToUse atk=$atkToUse")

                // observe antes de enfileirar (assegura collector ativo)
                observePrintingState()

                // chame versão suspend do PrintingHandler que enfileira (ou use o método existente que enfileira)
                printingHandler.enqueuePrintFiles(
                    printingViewModel = printingViewModel,
                    imageBitmap = getLatestPassBitmap(),
                    atk = atkToUse,
                    transactionId = transactionIdToUse,
                    passType = PassType.ProductCompact
                )

                // após enfileirar, inicie processamento
                printingViewModel.startProcessing()
                Log.d(TAG, "startPrintingProcess: startProcessing() chamado")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar impressão: ${e.message}", e)
                dismissLoadingModal()
                showErrorFragment("Erro ao iniciar impressão")
                printingStarted = false
            }
        }
    }

    private fun observePrintingState() {
        if (observingPrintingState) return
        observingPrintingState = true

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                printingViewModel.processingState.collect { state ->
                    when (state) {
                        is ProcessingState.QueueDone<*> -> {
                            dismissLoadingModal()
                            showSuccessModal(autoDismissMs = 1200L) {
                                shoppingCartManager.clearCart()
                                requireActivity().setResult(AppCompatActivity.RESULT_OK)
                                requireActivity().finish()
                            }
                        }
                        is ProcessingState.ItemFailed<*>, is ProcessingState.QueueAborted<*>, is ProcessingState.QueueCanceled<*> -> {
                            dismissLoadingModal()
                            showErrorModal { requireActivity().finish() }
                        }
                        is ProcessingState.ItemDone<*> -> {
                            if (state.result is PaymentSuccess) {
                                val paymentSuccess = state.result as PaymentSuccess
                                Log.d("PaymentState", "Printing ItemDone txId=${paymentSuccess.txId}")
                            }
                        }
                        else -> {
                            // ignora outros estados
                        }
                    }
                }
            }
        }
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

    private fun showSuccessModal(autoDismissMs: Long = 1200L, onDismiss: (() -> Unit)? = null) {
        successDialog = PrintingSuccessDialogFragment().apply {
            onFinishListener = object : PrintingSuccessDialogFragment.OnFinishListener {
                override fun onFinish() {
                    try { dismissAllowingStateLoss() } catch (_: Exception) {}
                    requireActivity().finish()
                    onDismiss?.invoke()
                }
            }
        }
        successDialog?.show(parentFragmentManager, "printing_success")
        successDialog?.dialog?.window?.decorView?.postDelayed({
            successDialog?.onFinishListener?.onFinish()
        }, autoDismissMs)
    }

    private fun showErrorModal(onDismiss: (() -> Unit)? = null) {
        errorDialog = PrintingErrorDialogFragment()
        errorDialog?.cancelPrintingListener = object : PrintingErrorDialogFragment.OnCancelPrintingListener {
            override fun onCancelPrinting() {
                printingViewModel.cancelAllPrintings()
                dismissLoadingModal()
                errorDialog?.dismissAllowingStateLoss()
                requireActivity().finish()
                onDismiss?.invoke()
            }
        }
        errorDialog?.show(parentFragmentManager, "printing_error")
    }

    private fun showErrorFragment(errorMessage: String) {
        val frag = PaymentErrorFragment.newInstance(errorMessage)
        replaceWithFragment(frag)
    }

    private fun replaceWithFragment(fragment: Fragment) {
        val containerId = requireActivity().findViewById<View?>(R.id.fragment_container)?.id ?: android.R.id.content
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
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

    private fun triggerPrintingIfReady() {
        Log.d(TAG, "triggerPrintingIfReady: finishPaymentHandled=$finishPaymentHandled, printingStarted=$printingStarted, lastTxId=$lastTxId")
        if (printingStarted) {
            Log.d(TAG, "triggerPrintingIfReady: já iniciou impressão, ignorando")
            return
        }
        if (!finishPaymentHandled) {
            Log.d(TAG, "triggerPrintingIfReady: aguardando finishPaymentHandled")
            return
        }
        if (lastTxId.isNullOrBlank()) {
            Log.d(TAG, "triggerPrintingIfReady: aguardando txId/atk")
            return
        }
        printingStarted = true
        Log.d(TAG, "triggerPrintingIfReady: condições atendidas, iniciando impressão")
        startPrintingProcess(lastTxId, lastAtk)
    }

    companion object {
        private const val TAG = "CardPaymentFragment"
    }
}