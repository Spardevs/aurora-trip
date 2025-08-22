package br.com.ticpass.pos.data.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Immutable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.APITestResponse
import br.com.ticpass.pos.data.event.ForYouViewModel
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.util.DeviceUtils.getDeviceSerial
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.toString
import com.airbnb.lottie.compose.LottieClipSpec
import com.google.android.gms.games.gamessignin.AuthResponse
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
    {},+
    1f,
)
@AndroidEntryPoint
class QrScannerActivity() : BaseActivity(), BarcodeCallback {
    @Inject
    lateinit var apiRepository: APIRepository

    private lateinit var barcodeView: BarcodeView

    companion object {
        private const val REQUEST_CAMERA = 1001
        private val FORMATS = listOf(BarcodeFormat.QR_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.zxing_barcode_scanner)
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
                doLogin(hash) { result ->
                result.onSuccess { response ->
                }.onFailure { error ->
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
                setResult(RESULT_OK, intent.putExtra("auth_response", authResponse.toString()))
                finish()
            } catch (e: Exception) {
                setResult(RESULT_CANCELED, intent.putExtra("auth_error", e.message))
                finish()
            }
        }
    }


}



