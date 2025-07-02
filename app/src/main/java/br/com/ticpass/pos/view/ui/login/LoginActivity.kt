package br.com.ticpass.pos.view.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.databinding.ActivityLoginBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            Log.d("LoginActivity", "QR lido: ${result.contents}")
        } else {
            Log.d("LoginActivity", "Leitura cancelada")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emailLoginButton.setOnClickListener {
            performEmailLogin()
        }

        binding.qrCodeLoginButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA
                )
            } else {
                startQrScanner()
            }
        }
    }

    private fun performEmailLogin() {
        Log.d("LoginActivity", "Perform login with email")
    }

    private fun performQrLogin() {

    }

    private fun startQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setCameraId(0)
            setPrompt("Aponte a câmera para o QR Code")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(QrScannerActivity::class.java)
        }
        barcodeLauncher.launch(options)
        val authResponse = withContext(Dispatchers.IO) {
            viewModel.apiRepository.login(
                emailState.text.replace("\\s".toRegex(), ""),
                passwordState.text,
                Build.SERIAL,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startQrScanner()
        } else {
            Log.w("LoginActivity", "Permissão de câmera negada")
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}