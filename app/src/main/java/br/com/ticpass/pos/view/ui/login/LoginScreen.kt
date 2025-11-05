package br.com.ticpass.pos.view.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
            tvSuccess?.text = successText ?: "Login validado"
            // Desabilitar botões aqui também, se quiser
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
            val error = result.data?.getStringExtra("auth_error") ?: "Erro desconhecido"
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

        // Inicialmente, campos de email e senha desabilitados
        binding.editTextTextEmailAddress.isEnabled = false
        binding.editTextTextPassword.isEnabled = false
        binding.buttonConfirm.isEnabled = false

        // Ao clicar no campo ou botão de email, habilita os campos para login
        binding.emailLoginButton.setOnClickListener {
            binding.choiceContainer.visibility = View.GONE
            binding.formContainer.visibility = View.VISIBLE

            binding.editTextTextEmailAddress.isEnabled = true
            binding.editTextTextPassword.isEnabled = true
            binding.buttonConfirm.isEnabled = true
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

            binding.editTextTextEmailAddress.isEnabled = false
            binding.editTextTextPassword.isEnabled = false
            binding.buttonConfirm.isEnabled = false
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
        val processingFragment = RefundProcessingFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, processingFragment)
            .commit()
        updateProcessingText(processingFragment)

        val handler = CoroutineExceptionHandler { _, throwable ->
            runOnUiThread {
                val errorFragment = RefundErrorFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, errorFragment)
                    .commit()
                updateErrorText(errorFragment)
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
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RefundSuccessFragment())
                        .commit()
                    startActivity(Intent(this@LoginScreen, MenuActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RefundErrorFragment())
                        .commit()
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

private fun updateProcessingText(fragment: Fragment) {
    fragment.view?.findViewById<TextView>(R.id.tv_processing)?.text = "Realizando login"
}

private fun updateSuccessText(fragment: Fragment) {
    fragment.view?.findViewById<TextView>(R.id.tv_success)?.text = "Login validado"
}

private fun updateErrorText(fragment: Fragment) {
    fragment.view?.findViewById<TextView>(R.id.tv_error)?.text = "Erro ao validar Login"
}