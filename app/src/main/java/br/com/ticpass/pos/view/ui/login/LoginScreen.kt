package br.com.ticpass.pos.view.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.activity.MenuActivity
import br.com.ticpass.pos.data.activity.QrScannerActivity
import br.com.ticpass.pos.databinding.ActivityLoginBinding
import com.auth0.android.jwt.JWT

class LoginScreen : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val response = result.data?.getStringExtra("auth_response")
            Log.d("LoginScreen", "Resposta RAW: $response")

            response?.let {
                try {
                    val token = extractValue(it, "token=")
                    val refreshToken = extractValue(it, "tokenRefresh=")
                    val userId = extractValue(it, "id=")?.toIntOrNull()
                    val userName = extractValue(it, "name=")
                    val tokenExpiration = token?.let { JWT(it).getClaim("exp").asDate() }

                    if (token != null && refreshToken != null && userId != null && tokenExpiration != null) {
                        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
                            putString("auth_token", token)
                            putString("refresh_token", refreshToken)
                            putString("token_expiration", tokenExpiration.toString())
                            putInt("user_id", userId)
                            putString("user_name", userName)
                            apply()
                        }
                        startActivity(Intent(this@LoginScreen, MenuActivity::class.java))
                        finish()
                    } else {
                        Log.e("LoginScreen", "Dados essenciais faltando na resposta")
                        showToast("Dados incompletos na resposta")
                    }
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Erro ao processar resposta: ${e.message}")
                    showToast("Erro ao processar resposta")
                }
            }
        } else {
            val error = result.data?.getStringExtra("auth_error")
            Log.e("LoginScreen", "Erro no login: $error")
            showToast("Falha no login: $error")
        }
    }

    private fun extractValue(source: String, key: String): String? {
        val startIndex = source.indexOf(key)
        if (startIndex == -1) return null

        val start = startIndex + key.length
        var end = source.indexOf(',', start)
        if (end == -1) end = source.indexOf(')', start)
        if (end == -1) return null

        return source.substring(start, end).trim().takeIf { it.isNotEmpty() }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun emailLoginButton(view: View) {
        binding.choiceContainer.visibility = View.GONE
        binding.formContainer.visibility = View.VISIBLE
    }

    fun onBackFromForm(view: View) {
        binding.formContainer.visibility = View.GONE
        binding.choiceContainer.visibility = View.VISIBLE
    }

    private fun startQrScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        scannerLauncher.launch(intent)
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
            startQrScanner()
        } else {
            showToast("Permissão de câmera é necessária")
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}