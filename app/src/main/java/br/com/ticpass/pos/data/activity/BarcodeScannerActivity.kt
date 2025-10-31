package br.com.ticpass.pos.data.activity

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.databinding.ActivityBarcodeScannerBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import timber.log.Timber

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScannerBinding
    private var hiddenEdit: EditText? = null

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
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)
        binding.zxingBarcodeScanner.decoderFactory = DefaultDecoderFactory(formats)

        binding.btnOpenKeyboard.setOnClickListener { showKeyboard() }
        binding.btnClose.setOnClickListener { finish() }

        if (hasCameraPermission()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
    }

    private fun startCamera() {
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                binding.zxingBarcodeScanner.pause()

                val barcodeInfo = validateAndReadBarcode(result)

                if (barcodeInfo != null) {
                    Timber.tag(TAG).i("✓ Barcode válido lido: ${barcodeInfo.text}")

                    displayBarcodeInfo(result)

                    showParsedBarcodeDialog(barcodeInfo.text)
                } else {
                    Timber.tag(TAG).w("✗ Código de barras inválido detectado")

                    binding.overlayPrompt.text = "✗ Código inválido\nTente novamente"

                    Toast.makeText(
                        this@BarcodeScannerActivity,
                        "Código de barras inválido. Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.zxingBarcodeScanner.postDelayed({
                        binding.overlayPrompt.text = "Posicione o código de barras"
                        binding.zxingBarcodeScanner.resume()
                    }, 1500)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        }
        binding.zxingBarcodeScanner.decodeContinuous(callback)
        binding.zxingBarcodeScanner.resume()
    }

    private fun showParsedBarcodeDialog(text: String?) {
        val payload = text ?: ""

        /**
         * Resolve paymentId a partir do payload lido.
         * - Se payload contiver '|', considera a primeira parte antes do '|'
         * - Remove sufixos como "-1" ou "-G" (split por '-' e pega primeiro segmento)
         * - Se o candidato for numérico com 13 dígitos (EAN-13), tenta lookup em SharedPreferences
         *   (chave: "map_<ean13>") e retorna o paymentId mapeado se existir.
         * - Caso contrário retorna o candidato bruto.
         */
        fun resolvePaymentIdFromPayload(ctx: Context, p: String): String? {
            if (p.isBlank()) return null
            val base = p.split("|").map { it.trim() }.firstOrNull() ?: return null
            val candidate = base.split("-").firstOrNull()?.trim() ?: base
            val numeric = candidate.replace("[^0-9]".toRegex(), "")

            if (numeric.length == 13) {
                return try {
                    val prefs = ctx.getSharedPreferences("BarcodeMappingPrefs", Context.MODE_PRIVATE)
                    val mapped = prefs.getString("map_$numeric", null)
                    if (!mapped.isNullOrBlank()) mapped else numeric
                } catch (e: Exception) {
                    numeric
                }
            }
            return candidate.takeIf { it.isNotBlank() }
        }

        val paymentIdResolved = resolvePaymentIdFromPayload(this, payload)
        Timber.tag(TAG).d("Parsed payload => resolvedPaymentId=$paymentIdResolved")
        Timber.tag(TAG).d("Scanner raw payload: $payload")

        val message = "paymentId: ${paymentIdResolved ?: "(não identificado)"}\n\nPayload original:\n$payload"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Informações lidas")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Copiar payload") { _, _ ->
                try {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("barcode_payload", payload)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Payload copiado", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Toast.makeText(this, "Erro ao copiar", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Usar") { _, _ ->
                // Ao usar, retornamos o paymentId resolvido (se disponível) para o caller.
                // Se não houver mapping/resolução, retornamos o payload original.
                val toReturn = paymentIdResolved ?: payload
                deliverResultAndFinish(toReturn)
            }
        builder.show()
    }

    private fun deliverResultAndFinish(text: String?) {
        val data = Intent().apply { putExtra(EXTRA_SCAN_TEXT, text ?: "") }
        setResult(RESULT_OK, data)
        finish()
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

        binding.overlayPrompt.text = "✓ $barcodeFormat\n$barcodeText"

        Toast.makeText(
            this,
            "Código lido: $barcodeText",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Valida e processa informações de um código de barras
     * @param barcodeResult Resultado do scan do barcode
     * @return BarcodeInfo se válido, null se inválido
     */
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
     * Classe de dados para informações do barcode
     */
    data class BarcodeInfo(
        val text: String,
        val format: String,
        val timestamp: Long,
        val isValid: Boolean,
        val metadata: Map<String, String> = emptyMap()
    )
}