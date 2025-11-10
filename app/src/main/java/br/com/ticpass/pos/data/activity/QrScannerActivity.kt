package br.com.ticpass.pos.data.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Immutable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.APITestResponse
import br.com.ticpass.pos.util.DeviceUtils.getDeviceSerial
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.airbnb.lottie.compose.LottieClipSpec
import com.journeyapps.barcodescanner.BarcodeCallback
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@Immutable
data class ScanStatus(
    val description: String,
    val iconResId: Int? = null,
    val onStart: () -> Unit,
    val onEnd: () -> Unit,
    val speed: Float = 1f,
    val iterations: Int = 1,
    val clipSpec: LottieClipSpec.Frame = LottieClipSpec.Frame()
)

private val initialScanStatus = ScanStatus(
    "Aponte para o seu ticpass ID",
    iconResId = null,
    {},
    {},
    1f,
)

@AndroidEntryPoint
class QrScannerActivity : BaseActivity(), BarcodeCallback {
    @Inject
    lateinit var apiRepository: APIRepository

    private lateinit var barcodeView: BarcodeView

    class RefundProcessingFragment : Fragment(R.layout.fragment_refund_processing) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val btnFinishSuccess = view.findViewById<Button>(R.id.btn_finish_success)
            val btnRetry = view.findViewById<Button>(R.id.btn_retry)
            btnFinishSuccess?.isEnabled = false
            btnRetry?.isEnabled = false
        }
    }

    class RefundErrorFragment : Fragment(R.layout.fragment_refund_error) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val btnFinishSuccess = view.findViewById<Button>(R.id.btn_finish_success)
            val btnRetry = view.findViewById<Button>(R.id.btn_retry)
            btnFinishSuccess?.isEnabled = false
            btnRetry?.isEnabled = false
        }
    }

    class RefundSuccessFragment : Fragment(R.layout.fragment_refund_success) {
        private var successText: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            successText = arguments?.getString("success_text")
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val tvSuccess = view.findViewById<TextView>(R.id.tv_success)
            tvSuccess?.text = successText ?: "Credenciais validadas com sucesso"
            val btnFinishSuccess = view.findViewById<Button>(R.id.btn_finish_success)
            val btnRetry = view.findViewById<Button>(R.id.btn_retry)
            btnFinishSuccess?.isEnabled = false
            btnRetry?.isEnabled = false
        }

        companion object {
            fun newInstance(successText: String): RefundSuccessFragment {
                val fragment = RefundSuccessFragment()
                val args = Bundle()
                args.putString("success_text", successText)
                fragment.arguments = args
                return fragment
            }
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
        private val FORMATS = listOf(com.google.zxing.BarcodeFormat.QR_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.zxing_qr_scanner)
        barcodeView.decoderFactory = DefaultDecoderFactory(FORMATS)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            startScanning()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            Log.w("QrScannerActivity", "Permissão de câmera negada")
            finish()
        }
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(this)
    }

    override fun barcodeResult(result: BarcodeResult) {
        barcodeView.pause()
        val text = result.text
        if (text != null && isPatternMatch(text)) {
            val hash = getHash(text)

            // Mostrar fragment de processamento
            val processingFragment = RefundProcessingFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, processingFragment)
                .commit()
            updateProcessingText(processingFragment)

            doLogin(hash) { result ->
                runOnUiThread {
                    result.onSuccess { response ->
                        val successFragment = RefundSuccessFragment.newInstance("Credenciais validadas com sucesso")
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, successFragment)
                            .commit()
                        updateSuccessText(successFragment)
                        setResult(RESULT_OK, intent.putExtra("auth_response", response.toString()))
                        finish()
                    }
                    result.onFailure { error ->
                        val errorFragment = RefundErrorFragment()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, errorFragment)
                            .commit()
                        updateErrorText(errorFragment)
                        Snackbar.make(barcodeView, "Erro: ${error.message}", Snackbar.LENGTH_SHORT).show()
                        setResult(RESULT_CANCELED, intent.putExtra("auth_error", error.message ?: "Erro desconhecido"))
                        finish()
                    }
                }
            }
        } else {
            showInvalidQrError()
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun isPatternMatch(input: String): Boolean {
        val pattern = Regex("^[0-9a-fA-F-]+@[0-9a-zA-Z]+\$")
        return pattern.matches(input)
    }

    fun getHash(input: String): String {
        val atIndex = input.indexOf("@")
        return input.substring(0, atIndex)
    }

    private fun showInvalidQrError() {
        Snackbar.make(barcodeView, "QR inválido: formato não reconhecido", Snackbar.LENGTH_SHORT).show()
    }

    fun getPin(input: String): String {
        val atIndex = input.indexOf("@")
        return input.substring(atIndex + 1)
    }

    fun doLogin(
        hash: String,
        onResult: (Result<APITestResponse>) -> Unit
    ) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            onResult(Result.failure(throwable))
        }

        lifecycleScope.launch(handler) {
            try {
                val authResponse = withContext(Dispatchers.IO) {
                    val serial = getDeviceSerial(this@QrScannerActivity)
                    apiRepository.loginQrcode(hash, serial)
                }
                onResult(Result.success(authResponse))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    /** 🔹 Atualizado conforme solicitado */
    private fun updateProcessingText(fragment: Fragment) {
        fragment.view?.findViewById<TextView>(R.id.tv_processing)?.text = "Validando credenciais"
    }

    private fun updateSuccessText(fragment: Fragment) {
        fragment.view?.findViewById<TextView>(R.id.tv_success)?.text = "Credenciais validadas com sucesso"
    }

    private fun updateErrorText(fragment: Fragment) {
        fragment.view?.findViewById<TextView>(R.id.tv_error)?.text = "Erro ao validar credenciais"
    }
}