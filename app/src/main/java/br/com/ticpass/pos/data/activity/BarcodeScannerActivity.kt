package br.com.ticpass.pos.data.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ActivityBarcodeScannerBinding
import br.com.ticpass.pos.feature.refund.RefundViewModel
import br.com.ticpass.pos.queue.processors.refund.processors.models.RefundProcessorType
import br.com.ticpass.pos.viewmodel.refund.PaymentRefundViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.fragment.app.commit
import br.com.ticpass.pos.view.fragments.refund.ProcessingFragment
import br.com.ticpass.pos.view.fragments.refund.SuccessFragment
import br.com.ticpass.pos.view.fragments.refund.ErrorFragment

@AndroidEntryPoint
class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarcodeScannerBinding
    private var hiddenEdit: EditText? = null
    private val paymentRefundViewModel: PaymentRefundViewModel by viewModels()
    private val refundViewModel: RefundViewModel by viewModels()

    companion object {
        private const val TAG = "CashPaymentFragment"
        private const val REQ_CAMERA = 2001
        const val EXTRA_SCAN_TEXT = "extra_scan_text"

        fun createIntent(context: Context): Intent =
            Intent(context, BarcodeScannerActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.overlayBarcode.setAspect(4f, 3f)
        binding.overlayBarcode.setMaxHeightFactor(0.38f)

        val formats = listOf(
            BarcodeFormat.CODE_128,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.ITF
        )
        binding.zxingBarcodeScanner.decoderFactory = DefaultDecoderFactory(formats)
        binding.btnOpenKeyboard.setOnClickListener { showKeyboard() }
        binding.btnClose.setOnClickListener { finish() }

        if (hasCameraPermission()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
    }

    /**
     * Cria um AlertDialog baseado no layout fragment_printing_success.xml e retorna:
     * Triple(dialog, statusTextView, progressBar)
     */
    private fun createDialogFromLayout(initialMessage: String): Triple<AlertDialog, TextView, ProgressBar?> {
        val view = layoutInflater.inflate(R.layout.fragment_printing_success, null)

        val ivCompleted = view.findViewById<ImageView>(R.id.iv_completed)
        val tvCompleted = view.findViewById<TextView>(R.id.tv_completed)
        val btnFinish = view.findViewById<View>(R.id.btn_finish)

        // Criar um ProgressBar dinamicamente e inseri-lo acima do TextView
        val parent = tvCompleted.parent
        var progressBar: ProgressBar? = null
        if (parent is ViewGroup) {
            progressBar = ProgressBar(this).apply {
                isIndeterminate = true
            }
            // adiciona o progressbar antes do tvCompleted
            val index = parent.indexOfChild(tvCompleted)
            parent.addView(progressBar, index)
        }

        tvCompleted.text = initialMessage
        btnFinish.visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        return Triple(dialog, tvCompleted, progressBar)
    }

    private fun showStatusFragment(fragment: androidx.fragment.app.Fragment) {
        runOnUiThread {
            val container = findViewById<FrameLayout?>(R.id.refundStatusContainer)
            if (container == null) {
                Timber.tag(TAG).w("refundStatusContainer não encontrado no layout");
                Toast.makeText(this, "Container de status ausente", Toast.LENGTH_SHORT).show()
                return@runOnUiThread
            }

            container.visibility = View.VISIBLE
            container.bringToFront()

            val tag = fragment.javaClass.simpleName
            Timber.tag(TAG).d("Mostrando fragment: $tag")

            try {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.refundStatusContainer, fragment, tag)
                    .commitNowAllowingStateLoss() // força execução imediata
                Timber.tag(TAG).d("Fragment $tag commitNowAllowingStateLoss executado")
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Erro ao mostrar fragment $tag")
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.refundStatusContainer, fragment, tag)
                }
            }
        }
    }

    fun hideStatusOverlay() {
        runOnUiThread {
            val container = findViewById<FrameLayout?>(R.id.refundStatusContainer)
            container?.visibility = View.GONE
            // remover fragment se existir
            val fm = supportFragmentManager
            val frag = fm.findFragmentById(R.id.refundStatusContainer)
            if (frag != null) {
                fm.beginTransaction().remove(frag).commitAllowingStateLoss()
                fm.executePendingTransactions()
            }
        }
    }


    /**
     * Processa o payload lido: resolve paymentId -> busca PaymentEntity -> enfileira e inicia refund.
     * Mostra dialog customizado usando fragment_printing_success.xml e atualiza conforme resultado.
     */
    private fun processScannedPayload(payload: String) {
        Timber.tag(TAG).d("processScannedPayload() chamado com payload=$payload")
        lifecycleScope.launch {
            try {
                showStatusFragment(ProcessingFragment.newInstance("Estornando venda\nAguarde..."))
                Timber.tag(TAG).d("Mostrou ProcessingFragment")
                paymentRefundViewModel.loadPaymentFromScanned(payload)
                val state = paymentRefundViewModel.uiState.first { it is PaymentRefundViewModel.UiState.Success || it is PaymentRefundViewModel.UiState.Error }
                Timber.tag(TAG).d("PaymentRefundViewModel retornou estado: $state")
                when (state) {
                    is PaymentRefundViewModel.UiState.Success -> {
                        Timber.tag(TAG).d("Estado SUCCESS, preparando refund")
                    }
                    is PaymentRefundViewModel.UiState.Error -> {
                        Timber.tag(TAG).d("Estado ERROR: ${state.message}")
                        showStatusFragment(ErrorFragment.newInstance(state.message))
                    }
                    else -> {
                        Timber.tag(TAG).d("Estado inesperado: $state")
                        showStatusFragment(ErrorFragment.newInstance("Erro desconhecido"))
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro em processScannedPayload")
                showStatusFragment(ErrorFragment.newInstance("Erro: ${e.message ?: "desconhecido"}"))
            }
        }
    }

    private fun startCamera() {
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                binding.zxingBarcodeScanner.pause()

                val barcodeInfo = validateAndReadBarcode(result)

                if (barcodeInfo != null) {
                    Timber.tag(TAG).i("✓ Barcode válido lido: ${barcodeInfo.text}")

                    displayBarcodeInfo(result)

                    // Ao ler com sucesso, iniciar o fluxo de estorno diretamente (sem mostrar modal com opções)
                    processScannedPayload(barcodeInfo.text)
                } else {
                    Timber.tag(TAG).w("✗ Código de barras inválido detectado")

                    Toast.makeText(
                        this@BarcodeScannerActivity,
                        "Código de barras inválido. Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.zxingBarcodeScanner.postDelayed({
                        binding.zxingBarcodeScanner.resume()
                    }, 1500)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        }
        binding.zxingBarcodeScanner.decodeContinuous(callback)
        binding.zxingBarcodeScanner.resume()
    }

    private fun showKeyboard() {
        val et = ensureHiddenEditText()
        et.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun ensureHiddenEditText(): EditText {
        hiddenEdit?.let { return it }
        val root = findViewById<ViewGroup>(android.R.id.content)
        val et = EditText(this).apply {
            tag = "hidden_edit_for_keyboard"
            visibility = View.INVISIBLE
            isFocusableInTouchMode = true
        }
        root.addView(et, ViewGroup.LayoutParams(1, 1))
        hiddenEdit = et
        return et
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            binding.zxingBarcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.zxingBarcodeScanner.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.zxingBarcodeScanner.pause()
        binding.zxingBarcodeScanner.stopDecoding()
    }

    private fun displayBarcodeInfo(result: BarcodeResult) {
        val barcodeText = result.text
        val barcodeFormat = result.barcodeFormat.toString()

        Timber.tag(TAG).d("════")
        Timber.tag(TAG).d("CÓDIGO DE BARRAS DETECTADO")
        Timber.tag(TAG).d("════")
        Timber.tag(TAG).d("Formato: $barcodeFormat")
        Timber.tag(TAG).d("Conteúdo: $barcodeText")
        Timber.tag(TAG).d("Timestamp: ${result.timestamp}")
        Timber.tag(TAG).d("════")

        Toast.makeText(
            this,
            "Código lido: $barcodeText",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun validateAndReadBarcode(barcodeResult: BarcodeResult?): BarcodeInfo? {
        if (barcodeResult == null) {
            Timber.tag("BarcodeValidator").w("Resultado do barcode é nulo")
            return null
        }

        val text = barcodeResult.text
        val format = barcodeResult.barcodeFormat

        if (text.isNullOrBlank()) {
            Timber.tag("BarcodeValidator").w("Texto do barcode está vazio")
            return null
        }

        Timber.tag("BarcodeValidator").i("✓ Barcode lido: formato=$format, texto=$text")

        return BarcodeInfo(
            text = text,
            format = format.toString(),
            timestamp = barcodeResult.timestamp,
            isValid = true
        )
    }

    /**
     * Chamado pelo ErrorFragment para retomar o scanner depois de um erro.
     */
    fun resumeScannerAfterError() {
        hideStatusOverlay()
        binding.zxingBarcodeScanner.post { binding.zxingBarcodeScanner.resume() }
    }

    data class BarcodeInfo(
        val text: String,
        val format: String,
        val timestamp: Long,
        val isValid: Boolean,
        val metadata: Map<String, String> = emptyMap()
    )
}