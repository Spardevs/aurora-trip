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
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.activity.MenuActivity
import br.com.ticpass.pos.data.activity.QrScannerActivity
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.APITestResponse
import br.com.ticpass.pos.databinding.ActivityLoginBinding
import br.com.ticpass.pos.util.DeviceUtils.getDeviceSerial
import com.auth0.android.jwt.JWT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoginScreen : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject
    lateinit var apiRepository: APIRepository

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

        binding.buttonConfirm.setOnClickListener {
            val username = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()
            val serial = getDeviceSerial(this)
            if (username.isBlank() || password.isBlank()) {
                showToast("Preencha usuário e senha")
            } else {
                doLogin(username, password, serial)
            }
        }

        binding.buttonBack.setOnClickListener {
            binding.choiceContainer.visibility = View.VISIBLE
            binding.formContainer.visibility = View.GONE

            binding.editTextTextEmailAddress.text.clear()
            binding.editTextTextPassword.text.clear()
        }

    }


    private fun doLogin(username: String, password: String, serial: String) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            runOnUiThread {
                showToast("Erro no login: ${throwable.message}")
            }
        }

        lifecycleScope.launch(handler) {
            try {
                val authResponse: APITestResponse = withContext(Dispatchers.IO) {
                    apiRepository.login(username, password, serial)
                }
                runOnUiThread {
                    saveAuthData(authResponse)
                    startActivity(Intent(this@LoginScreen, MenuScreen::class.java))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Falha no login: ${e.message}")
                }
            }
        }
    }

    private fun saveAuthData(authResponse: APITestResponse) {
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("auth_token", authResponse.result.token)
            putString("refresh_token", authResponse.result.tokenRefresh)
            putInt("user_id", authResponse.result.user.id.toIntOrNull() ?: -1)
            putString("user_name", authResponse.result.user.name)
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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