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

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager
    @Inject
    lateinit var finishPaymentHandler: FinishPaymentHandler
    @Inject
    lateinit var paymentUtils: PaymentFragmentUtils

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
    private var paymentValue: Double = 0.0
    private var totalValue: Double = 0.0
    private var remainingValue: Double = 0.0
    private var isMultiPayment: Boolean = false
    private var progress: String = ""

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
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::shoppingCartManager.isInitialized) {
            Log.e(TAG, "ShoppingCartManager not initialized")
            requireActivity().finish()
            return
        }

        setupUI(view)
        setupPaymentEventHandler(view)
        setupPrintingHandler()
        setupObservers()

        setFragmentResultListener("retry_payment") { _, _ ->
            retryPayment()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::shoppingCartManager.isInitialized) {
            enqueueAndStartPayment()
        }
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

        if (paymentType.isNullOrBlank()) {
            val methodFromIntent = requireActivity().intent.getStringExtra("payment_type")
            if (!methodFromIntent.isNullOrBlank()) {
                paymentType = methodFromIntent
                Log.d(TAG, "payment_type obtido do Intent: $paymentType")
            }
        }

        paymentType = paymentType?.lowercase()
        if (paymentType != "credit_card" && paymentType != "debit_card") {
            Log.w(TAG, "payment_type ausente/ambíguo/inválido ($paymentType). Aplicando default: credit_card")
            paymentType = "credit_card"
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

    private fun setupPrintingHandler() {
        printingHandler = PrintingHandler(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner
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
        pinInputLayout = view.findViewById(R.id.pin_input_layout)
        pinInput = view.findViewById(R.id.pin_input)
        submitPinButton = view.findViewById(R.id.btn_submit_pin)

        priceTextView.text = PaymentFragmentUtils.formatCurrency(paymentValue)

        if (isMultiPayment) {
            val progressTextView = view.findViewById<TextView>(R.id.tv_progress)
            progressTextView?.visibility = View.VISIBLE
            progressTextView?.text = "Pagamento $progress"
        }

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

        retryButton.visibility = View.GONE
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
            isPix = false
        )
    }

    private fun enqueueAndStartPayment() {
        val method = when (paymentType) {
            "credit_card" -> SystemPaymentMethod.CREDIT
            "debit_card" -> SystemPaymentMethod.DEBIT
            else -> {
                Log.e(TAG, "Tipo de pagamento inválido: $paymentType")
                showErrorFragment("Tipo de pagamento inválido")
                return
            }
        }

        PaymentFragmentUtils.enqueuePayment(
            paymentViewModel = paymentViewModel,
            shoppingCartManager = shoppingCartManager,
            method = method,
            amount = paymentValue,
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
        activity?.runOnUiThread {
            retryButton.visibility = View.GONE
            statusTextView.text = "Preparando nova tentativa..."
            infoTextView.text = "Aguarde..."
            imageView.setImageResource(
                when (paymentType) {
                    "credit_card" -> R.drawable.ic_credit_card
                    "debit_card" -> R.drawable.ic_credit_card
                    else -> R.drawable.ic_credit_card
                }
            )
            imageView.clearColorFilter()

            Handler(Looper.getMainLooper()).postDelayed({
                enqueueAndStartPayment()
            }, 1000)
        }
    }

    private fun showErrorFragment(errorMessage: String) {
        val frag = PaymentErrorFragment.newInstance(errorMessage)
        replaceWithFragment(frag)
    }

    private fun replaceWithFragment(fragment: Fragment) {
        val containerId = requireActivity().findViewById<View?>(R.id.fragment_container)?.id
            ?: android.R.id.content
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
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

                startPrintingProcess()

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao finalizar pagamento", e)
                dismissLoadingModal()
                showErrorFragment("Erro ao finalizar pagamento")
            }
        }
    }

    private fun startPrintingProcess() {
        try {
            AcquirerSdk.initialize(requireContext())

            val latestBitmap = getLatestPassBitmap()
            printingHandler.enqueueAndStartPrinting(
                printingViewModel = printingViewModel,
                imageBitmap = latestBitmap
            )
            observePrintingState()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar impressão: ${e.message}")
            dismissLoadingModal()
            showErrorFragment("Erro ao iniciar impressão")
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

                        is ProcessingState.ItemFailed<*> -> {
                            dismissLoadingModal()
                            showErrorModal {
                                requireActivity().finish()
                            }
                        }

                        is ProcessingState.QueueAborted<*>,
                        is ProcessingState.QueueCanceled<*> -> {
                            dismissLoadingModal()
                            showErrorModal {
                                requireActivity().finish()
                            }
                        }

                        is ProcessingState.ItemDone<*> -> {

                            val result = state.result as PaymentSuccess

                            Log.d("Teste", "Teste: ${state.item}")
                            Log.d("Teste", "Teste: ${result.atk}")

                            dismissLoadingModal()
                            showSuccessModal(autoDismissMs = 1200L)
                        }

                        else -> {}
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
                    try {
                        dismissAllowingStateLoss()
                    } catch (_: Exception) {}
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

    companion object {
        private const val TAG = "CardPaymentFragment"
    }
}