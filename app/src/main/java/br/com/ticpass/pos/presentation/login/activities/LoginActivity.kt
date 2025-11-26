package br.com.ticpass.pos.presentation.login.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.auth.repository.AuthRepositoryImpl
import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.data.user.local.entity.UserEntity
import br.com.ticpass.pos.presentation.scanners.contracts.QrScannerContract
import br.com.ticpass.pos.presentation.login.fragments.LoadingLoginFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse

@AndroidEntryPoint
class LoginActivity : AppCompatActivity(), LoadingLoginFragment.LoadingListener {

    @Inject
    lateinit var authRepository: AuthRepositoryImpl

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var qrLoginButton: Button

    private val qrScannerLauncher = registerForActivityResult(QrScannerContract()) { qrText ->
        if (qrText == null) {
            Toast.makeText(this, "Leitura do QR cancelada ou inválida", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        performQrLogin(qrText)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.editTextTextEmailAddress)
        passwordEditText = findViewById(R.id.editTextTextPassword)
        loginButton = findViewById(R.id.button_confirm)
        qrLoginButton = findViewById(R.id.qr_code_login_button)

        loginButton.setOnClickListener { performEmailLogin() }
        qrLoginButton.setOnClickListener { startQrLogin() }
    }

    private fun startQrLogin() {
        qrScannerLauncher.launch(Unit)
    }

    private fun performEmailLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val handler = CoroutineExceptionHandler { _, throwable ->
            Timber.tag("LoginActivity").e(throwable, "Exception no handler (email login)")
            runOnUiThread {
                Toast.makeText(this, "Erro de conexão: ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch(handler) {
            try {
                Timber.tag("LoginActivity").d("Iniciando login por e-mail")

                val response: Response<LoginResponse> = withContext(Dispatchers.IO) {
                    authRepository.signIn(email, password)
                }

                Timber.tag("LoginActivity").d("HTTP=${response.code()} success=${response.isSuccessful}")

                if (response.isSuccessful && (response.code() == 200 || response.code() == 201)) {
                    val body = response.body()
                    if (body == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Resposta vazia do servidor", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val cookies = response.headers().values("Set-Cookie")
                    val accessToken = cookies.firstOrNull { it.startsWith("access=") }
                        ?.substringAfter("access=")?.substringBefore(";")
                        ?: body.jwt.access

                    val refreshToken = cookies.firstOrNull { it.startsWith("refresh=") }
                        ?.substringAfter("refresh=")?.substringBefore(";")
                        ?: body.jwt.refresh

                    saveUserDataLocally(body, accessToken, refreshToken)

                    showLoadingFragment("Carregando suas preferências...")

                } else {
                    handleErrorResponse(response)
                }
            } catch (e: Exception) {
                Timber.tag("LoginActivity").e(e, "Erro no login por e-mail")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Erro no login: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performQrLogin(qrText: String) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            Timber.tag("LoginActivity").e(throwable, "Exception no handler (QR login)")
            runOnUiThread {
                Toast.makeText(this, "Erro no login via QR: ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch(handler) {
            try {
                val parts = qrText.split("@")
                if (parts.size != 2) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "QR inválido: formato esperado token@pin", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val token = parts[0].trim()
                val pin = parts[1].trim()

                Timber.tag("LoginActivity").d("Chamando authRepository.signInWithQrCode()")
                val response: Response<LoginResponse> = withContext(Dispatchers.IO) {
                    authRepository.signInWithQrCode(token, pin)
                }

                Timber.tag("LoginActivity").d("HTTP=${response.code()} success=${response.isSuccessful}")

                if (response.isSuccessful && (response.code() == 200 || response.code() == 201)) {
                    val body = response.body()
                    if (body == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Resposta vazia do servidor", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val cookies = response.headers().values("Set-Cookie")
                    val accessToken = cookies.firstOrNull { it.startsWith("access=") }
                        ?.substringAfter("access=")?.substringBefore(";")
                        ?: body.jwt.access

                    val refreshToken = cookies.firstOrNull { it.startsWith("refresh=") }
                        ?.substringAfter("refresh=")?.substringBefore(";")
                        ?: body.jwt.refresh

                    saveUserDataLocally(body, accessToken, refreshToken)

                    showLoadingFragment("Carregando dados do usuário...")

                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Requisição inválida"
                        401 -> "E-mail, token ou PIN incorretos"
                        in 500..599 -> "Erro no servidor"
                        else -> "Erro desconhecido (status ${response.code()})"
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Timber.tag("LoginActivity").e(e, "Erro no login via QR")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Erro no login via QR: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoadingFragment(message: String) {
        val frag = LoadingLoginFragment.newInstance(message)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, frag, "loading_fragment")
            .commitAllowingStateLoss()
    }

    override fun onLoadingFinished() {
        val frag = supportFragmentManager.findFragmentByTag("loading_fragment")
        if (frag != null) {
            supportFragmentManager.beginTransaction().remove(frag).commitAllowingStateLoss()
        }

//        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    private suspend fun handleErrorResponse(response: Response<LoginResponse>) {
        val errorMsg = when (response.code()) {
            400 -> "Requisição inválida"
            401 -> "E-mail ou senha incorretos"
            in 500..599 -> "Erro no servidor"
            else -> "Erro desconhecido (status ${response.code()})"
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUserDataLocally(login: LoginResponse, accessToken: String?, refreshToken: String?) {
        val userDao = AppDatabase.getDatabase(this).userDao()
        val userEntity = UserEntity(
            id = login.user.id,
            accessToken = accessToken ?: login.jwt.access,
            refreshToken = refreshToken ?: login.jwt.refresh
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                userDao.insert(userEntity)
            }
        }
        Timber.tag("LoginActivity").d("Dados de autenticação salvos")
    }
}