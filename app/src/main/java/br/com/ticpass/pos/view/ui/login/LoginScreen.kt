package br.com.ticpass.pos.view.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.data.activity.MenuActivity
import br.com.ticpass.pos.data.activity.QrScannerActivity
import br.com.ticpass.pos.databinding.ViewLoginBinding
import org.json.JSONException
import org.json.JSONObject

class LoginScreen : AppCompatActivity() {
    private lateinit var binding: ViewLoginBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuScreen

    private val scannerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val response = result.data?.getStringExtra("auth_response")
            Log.d("LoginScreen", "Resposta RAW: $response")

            response?.let {
                try {
                    val token = extractValue(it, "token=")
                    val refreshToken = extractValue(it, "tokenRefresh=")
                    val userId = extractValue(it, "id=")?.toIntOrNull()
                    if (token != null && refreshToken != null && userId != null) {
                        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("auth_token", token)
                            putString("refresh_token", refreshToken)
                            putInt("user_id", userId)
                            apply()
                        }
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    } else {
                        Log.e("LoginScreen", "Dados essenciais faltando na resposta")
                        Toast.makeText(this, "Dados incompletos na resposta", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Erro ao processar resposta: ${e.message}")
                    Toast.makeText(this, "Erro ao processar resposta", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val error = result.data?.getStringExtra("auth_error")
            Log.e("LoginScreen", "Erro no login: $error")
            Toast.makeText(this, "Falha no login: $error", Toast.LENGTH_SHORT).show()
        }
    }

    // Função auxiliar para extrair valores do formato string do objeto
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
        binding = ViewLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emailLoginButton.setOnClickListener {
            // performEmailLogin()
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

    private fun startQrScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        scannerLauncher.launch(intent)
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
            Log.w("LoginScreen", "Permissão de câmera negada")
            Toast.makeText(this, "Permissão de câmera é necessária", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}