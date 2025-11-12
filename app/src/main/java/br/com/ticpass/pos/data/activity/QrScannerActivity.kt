package br.com.ticpass.pos.data.activity

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Api2Repository
import br.com.ticpass.pos.data.api.ShortLivedSignInResponse
import br.com.ticpass.pos.view.fragments.qrcode.QrCodeErrorFragment
import br.com.ticpass.pos.view.fragments.qrcode.QrCodeProcessingFragment
import br.com.ticpass.pos.view.fragments.qrcode.QrCodeSuccessFragment
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.BarcodeCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class QrScannerActivity : BaseActivity(), BarcodeCallback {

    @Inject
    lateinit var api2Repository: Api2Repository

    private lateinit var barcodeView: BarcodeView

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
        val text = result.text?.trim()
        Log.d("QrScannerActivity", "QR Code lido: $text")

        if (text.isNullOrBlank() || !text.contains("@")) {
            showErrorFragment("QR inválido: formato não reconhecido (esperado: token@pin)")
            barcodeView.resume()
            return
        }

        val processingFragment = QrCodeProcessingFragment()
        showFragment(processingFragment)

        doLoginV2(text) { result ->
            runOnUiThread {
                result.onSuccess { response ->
                    Log.d("QrScannerActivity", "Login bem-sucedido! Token: ${response.result?.token}")
                    showFragment(QrCodeSuccessFragment())

                    // Passa os dados de autenticação para a Activity que chamou
                    intent.putExtra("auth_token", response.result?.token)
                    intent.putExtra("auth_refresh_token", response.result?.refreshToken)
                    intent.putExtra("auth_expires_in", response.result?.expiresIn)

                    setResult(RESULT_OK, intent)
                    finish()
                }
                result.onFailure { e ->
                    Log.e("QrScannerActivity", "Falha no login: ${e.message}")
                    showFragment(QrCodeErrorFragment())
                    Snackbar.make(barcodeView, "Erro: ${e.message}", Snackbar.LENGTH_LONG).show()
                    setResult(RESULT_CANCELED, intent.putExtra("auth_error", e.message))
                    finish()
                }
            }
        }
    }

    private fun doLoginV2(qrText: String, onResult: (Result<ShortLivedSignInResponse>) -> Unit) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            Log.e("QrScannerActivity", "Exception no handler", throwable)
            onResult(Result.failure(throwable))
        }

        lifecycleScope.launch(handler) {
            try {
                val parts = qrText.split("@")
                if (parts.size != 2) {
                    throw Exception("Formato inválido de QRCode. Esperado: token@pin")
                }

                val token = parts[0].trim()
                val pin = parts[1].trim()

                Log.d("QrScannerActivity", "Token extraído: $token")
                Log.d("QrScannerActivity", "PIN extraído: $pin")
                Log.d("QrScannerActivity", "Chamando Api2Repository.signInShortLived()")

                val response = withContext(Dispatchers.IO) {
                    api2Repository.signInShortLived(token, pin)
                }

                Log.d(
                    "QrScannerActivity",
                    "Response: status=${response.status}, msg=${response.message}, error=${response.error}"
                )

                // ✅ Aceita status 200 E 201 como sucesso
                when {
                    response.status in listOf(200, 201) && response.result?.token != null -> {
                        Log.d("QrScannerActivity", "Autenticação bem-sucedida (status ${response.status})")
                        onResult(Result.success(response))
                    }
                    response.status == 401 -> {
                        Log.w("QrScannerActivity", "Erro 401: Token ou PIN inválido")
                        onResult(Result.failure(Exception("Não autorizado: Token ou PIN inválido")))
                    }
                    response.status == 400 -> {
                        Log.w("QrScannerActivity", "Erro 400: Requisição inválida")
                        onResult(Result.failure(Exception("Requisição inválida: ${response.message ?: response.error}")))
                    }
                    response.status >= 500 -> {
                        Log.e("QrScannerActivity", "Erro 5xx: Problema no servidor")
                        onResult(Result.failure(Exception("Erro no servidor: ${response.message ?: response.error}")))
                    }
                    else -> {
                        Log.e("QrScannerActivity", "Erro desconhecido: status=${response.status}")
                        onResult(Result.failure(Exception(response.message ?: response.error ?: "Erro desconhecido (status ${response.status})")))
                    }
                }
            } catch (e: Exception) {
                Log.e("QrScannerActivity", "Erro no login", e)
                onResult(Result.failure(e))
            }
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

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
    }

    private fun showErrorFragment(message: String) {
        val frag = QrCodeErrorFragment()
        showFragment(frag)
        Snackbar.make(barcodeView, message, Snackbar.LENGTH_LONG).show()
    }
}