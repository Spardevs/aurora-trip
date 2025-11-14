package br.com.ticpass.pos.data.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.Constants
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Api2Repository
import br.com.ticpass.pos.data.api.LoginResponse
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
import retrofit2.Response
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

        doLoginV2(text) { outcome ->
            runOnUiThread {
                outcome.onSuccess { loginData ->
                    val (loginResponse, accessToken, refreshToken) = loginData
                    Log.d(
                        "QrScannerActivity",
                        "Login bem-sucedido! accessToken=${accessToken?.take(10)}..."
                    )

                    // ✅ SALVA OS DADOS NO SHAREDPREFERENCES
                    saveAuthDataV2(loginResponse, accessToken, refreshToken)

                    showFragment(QrCodeSuccessFragment())

                    // ✅ REDIRECIONA PARA MENU
                    startActivity(Intent(this@QrScannerActivity, MenuActivity::class.java))
                    finish()
                }
                outcome.onFailure { e ->
                    Log.e("QrScannerActivity", "Falha no login: ${e.message}")
                    showFragment(QrCodeErrorFragment())
                    Snackbar.make(barcodeView, "Erro: ${e.message}", Snackbar.LENGTH_LONG).show()

                    // Retorna erro para LoginScreen (se foi chamado de lá)
                    setResult(RESULT_CANCELED, intent.putExtra("auth_error", e.message))

                    // Aguarda 2 segundos e volta para a tela de login
                    lifecycleScope.launch {
                        delay(2000)
                        finish()
                    }
                }
            }
        }
    }

    private fun doLoginV2(
        qrText: String,
        onResult: (Result<Triple<LoginResponse, String?, String?>>) -> Unit
    ) {
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
                    "HTTP=${response.code()} success=${response.isSuccessful}"
                )

                if (response.isSuccessful && (response.code() == 200 || response.code() == 201)) {
                    val body = response.body()
                    if (body == null) {
                        onResult(Result.failure(Exception("Resposta vazia do servidor")))
                        return@launch
                    }

                    // Extrai cookies Set-Cookie: access=...; ..., refresh=...; ...
                    val cookies = response.headers().values("Set-Cookie")
                    val accessCookie = cookies.firstOrNull { it.startsWith("access=") }
                    val refreshCookie = cookies.firstOrNull { it.startsWith("refresh=") }

                    val accessToken = accessCookie
                        ?.substringAfter("access=")
                        ?.substringBefore(";")
                        ?.takeIf { it.isNotBlank() }
                        ?: body.jwt.access // Fallback para o body se não vier no cookie

                    val refreshToken = refreshCookie
                        ?.substringAfter("refresh=")
                        ?.substringBefore(";")
                        ?.takeIf { it.isNotBlank() }
                        ?: body.jwt.refresh // Fallback para o body se não vier no cookie

                    Log.d(
                        "QrScannerActivity",
                        "Tokens extraídos: access=${accessToken != null} refresh=${refreshToken != null}"
                    )

                    onResult(Result.success(Triple(body, accessToken, refreshToken)))
                } else if (response.code() == 401) {
                    onResult(Result.failure(Exception("Não autorizado: Token ou PIN inválido")))
                } else if (response.code() == 400) {
                    onResult(Result.failure(Exception("Requisição inválida")))
                } else if (response.code() >= 500) {
                    onResult(Result.failure(Exception("Erro no servidor")))
                } else {
                    onResult(Result.failure(Exception("Erro desconhecido (status ${response.code()})")))
                }
            } catch (e: Exception) {
                Log.e("QrScannerActivity", "Erro no login", e)
                onResult(Result.failure(e))
            }
        }
    }

    private fun saveAuthDataV2(login: LoginResponse, accessToken: String?, refreshToken: String?) {
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("auth_token", accessToken ?: login.jwt.access)
            putString("refresh_token", refreshToken ?: login.jwt.refresh)
            putInt("user_id", -1) // ou extrair do login.user.id se for String → Int
            putString("user_name", login.user.name)
            putString("proxy_credentials", Constants.PROXY_CREDENTIALS)
            apply()
        }
        Log.d("QrScannerActivity", "Dados de autenticação salvos no SharedPreferences")
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