package br.com.ticpass.pos.view.fragments.payment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.printing.events.PrintingHandler
import br.com.ticpass.pos.queue.models.PaymentSuccess
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.view.fragments.printing.PrintingErrorDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingLoadingDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingSuccessDialogFragment
import br.com.ticpass.pos.view.ui.pass.PassType
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import javax.inject.Inject
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class PaymentSuccessFragment : Fragment() {

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    private val printingViewModel: PrintingViewModel by activityViewModels()
    private lateinit var printingHandler: PrintingHandler

    private var isMultiPayment: Boolean = false
    private var progress: String = ""
    private var txId: String = ""
    private var atk: String = ""

    private var loadingDialog: PrintingLoadingDialogFragment? = null
    private var successDialog: PrintingSuccessDialogFragment? = null
    private var errorDialog: PrintingErrorDialogFragment? = null
    private var observingPrintingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isMultiPayment = it.getBoolean(ARG_MULTI, false)
            progress = it.getString(ARG_PROGRESS, "")
            txId = it.getString(ARG_TX_ID, "") ?: ""
            atk = it.getString(ARG_ATK, "") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_success, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        printingHandler = PrintingHandler(requireContext(), viewLifecycleOwner)

        val btnFinish = view.findViewById<MaterialButton>(R.id.btn_finish)

        btnFinish?.setOnClickListener {
            if (isMultiPayment) {
                requireActivity().setResult(AppCompatActivity.RESULT_OK)
                requireActivity().finish()
            } else {
                shoppingCartManager.clearCart()
                requireActivity().finish()
            }
        }

        startPrintingProcess(txId.takeIf { it.isNotBlank() }, atk.takeIf { it.isNotBlank() })
    }

    private fun startPrintingProcess(transactionId: String? = null, atk: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                observePrintingState()

                printingHandler.enqueuePrintFiles(
                    printingViewModel = printingViewModel,
                    imageBitmap = getLatestPassBitmap(),
                    atk = atk,
                    transactionId = transactionId,
                    passType = PassType.ProductCompact
                )

                showLoadingModal()
                printingViewModel.startProcessing()
                Log.d(TAG, "startPrintingProcess: startProcessing() chamado")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar impressão: ${e.message}", e)
                dismissLoadingModal()
                showErrorModal { requireActivity().finish() }
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
                                Log.d("PrintingState", "Printing ItemDone txId=${paymentSuccess.txId}")
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }
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

    companion object {
        private const val ARG_MULTI = "is_multi_payment"
        private const val ARG_PROGRESS = "progress"
        private const val ARG_TX_ID = "tx_id"
        private const val ARG_ATK = "atk"
        private const val TAG = "PaymentSuccessFragment"

        @JvmStatic
        fun newInstance(isMultiPayment: Boolean, progress: String, txId: String, atk: String) =
            PaymentSuccessFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MULTI, isMultiPayment)
                    putString(ARG_PROGRESS, progress)
                    putString(ARG_TX_ID, txId)
                    putString(ARG_ATK, atk)
                }
            }
    }
}