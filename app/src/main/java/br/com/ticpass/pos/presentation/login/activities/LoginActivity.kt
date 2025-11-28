package br.com.ticpass.pos.presentation.login.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.auth.repository.AuthRepositoryImpl
import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.data.user.local.entity.UserEntity
import br.com.ticpass.pos.presentation.scanners.contracts.QrScannerContract
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse
import br.com.ticpass.pos.presentation.scanners.fragments.QrCodeErrorFragment
import br.com.ticpass.pos.presentation.scanners.fragments.QrCodeProcessingFragment
import br.com.ticpass.pos.presentation.scanners.fragments.QrCodeSuccessFragment
import br.com.ticpass.pos.presentation.scanners.states.QrLoginState
import br.com.ticpass.pos.presentation.scanners.viewmodels.QrLoginViewModel

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepositoryImpl
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var qrLoginButton: Button
    private lateinit var loginErrorText: TextView
    private lateinit var choiceContainer: View
    private lateinit var formContainer: View
    private lateinit var emailChoiceButton: Button
    private lateinit var buttonBack: MaterialButton
    private lateinit var fallingImg: ImageView
    private var fallingOriginalTranslationY: Float = 0f
    private val fallingUpOffsetDp = 190

    // Use apenas uma instância do ViewModel
    private val qrViewModel: QrLoginViewModel by viewModels()

    private val qrScannerLauncher = registerForActivityResult(QrScannerContract()) { qrText ->
        if (qrText == null) {
            Toast.makeText(this, "Leitura do QR cancelada ou inválida", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        qrViewModel.signInWithQr(qrText)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        choiceContainer = findViewById(R.id.choiceContainer)
        formContainer = findViewById(R.id.formContainer)
        ViewCompat.setTranslationZ(formContainer, 30f)
        emailChoiceButton = findViewById(R.id.email_login_button)
        buttonBack = findViewById(R.id.button_back)

        fallingImg = findViewById(R.id.falling)
        fallingImg.isClickable = false
        fallingImg.translationY = 0f
        fallingImg.post {
            fallingOriginalTranslationY = fallingImg.translationY
        }

        val deviceInfoImage = findViewById<ImageView>(R.id.deviceInfo)
        deviceInfoImage.setOnClickListener { showDeviceInfoDialog() }

        emailEditText = findViewById(R.id.editTextTextEmailAddress)
        passwordEditText = findViewById(R.id.editTextTextPassword)
        loginButton = findViewById(R.id.button_confirm)
        qrLoginButton = findViewById(R.id.qr_code_login_button)
        loginErrorText = findViewById(R.id.login_error_text)

        // TextWatcher: limpa erros quando usuário digita
        val clearErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (loginErrorText.visibility == View.VISIBLE) {
                    loginErrorText.visibility = View.GONE
                    resetFieldState(emailEditText)
                    resetFieldState(passwordEditText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        emailEditText.addTextChangedListener(clearErrorWatcher)
        passwordEditText.addTextChangedListener(clearErrorWatcher)

        emailChoiceButton.setOnClickListener {
            choiceContainer.visibility = View.GONE
            formContainer.visibility = View.VISIBLE
            animateFalling(raise = true)
            emailEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(emailEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        buttonBack.setOnClickListener {
            animateFalling(raise = false)
            formContainer.visibility = View.GONE
            choiceContainer.visibility = View.VISIBLE

            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }

        // CLICK: confirmar login (efetua signIn) - agora faz validação antes
        loginButton.setOnClickListener {
            if (validateInputs()) {
                performEmailLogin()
            }
        }

        // CLICK: iniciar leitor QR
        qrLoginButton.setOnClickListener { startQrLogin() }

        // Observa o state do ViewModel para exibir os fragments de status
        lifecycleScope.launch {
            qrViewModel.state.collect { state ->
                when (state) {
                    is QrLoginState.Processing -> {
                        showFragment(QrCodeProcessingFragment())
                    }
                    is QrLoginState.Error -> {
                        // mostrar fragment de erro; você pode passar a mensagem via args se quiser
                        val frag = QrCodeErrorFragment()
                        val args = Bundle().apply { putString("qr_error_message", state.message) }
                        frag.arguments = args
                        showFragment(frag)
                    }
                    is QrLoginState.Success -> {
                        showFragment(QrCodeSuccessFragment())
                        // pequeno delay para o usuário ver o sucesso
                        launch {
                            delay(800)
                            // Aqui navegamos para LoginMenuActivity
                            onLoadingFinished()
                        }
                    }
                    is QrLoginState.Idle -> {
                        removeLoadingFragmentIfExists()
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }

    private fun animateFalling(raise: Boolean) {
        val offset = dpToPx(fallingUpOffsetDp)
        val target = if (raise) fallingOriginalTranslationY - offset else fallingOriginalTranslationY
        fallingImg.animate()
            .translationY(target)
            .setDuration(250)
            .start()
    }

    private fun resetFieldState(field: EditText) {
        try {
            field.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            field.background?.let { DrawableCompat.setTint(it, ContextCompat.getColor(this, R.color.colorWhite)) }
        } catch (e: Exception) {
        }
    }

    private fun highlightFieldError(field: EditText) {
        try {
            // pinta o contorno e o texto do campo em vermelho
            field.setTextColor(ContextCompat.getColor(this, R.color.colorRedAccent))
        } catch (e: Exception) {
            // fallback
        }
    }

    private fun showInputError(message: String, field: EditText?) {
        // Mostrar mensagem no campo de erro abaixo dos inputs
        loginErrorText.text = message
        loginErrorText.visibility = View.VISIBLE

        // Destacar o campo com erro (contorno e texto em vermelho, fundo branco)
        field?.let {
            highlightFieldError(it)
            it.requestFocus()
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty()) {
            showInputError("E-mail obrigatório", emailEditText)
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showInputError("E-mail inválido", emailEditText)
            return false
        }

        if (password.isEmpty()) {
            showInputError("Senha obrigatória", passwordEditText)
            return false
        }

        return true
    }

    fun startQrLogin() {
        qrScannerLauncher.launch(Unit)
    }

    private fun showDeviceInfoDialog() {
        // Infla o layout do dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_device_info, null)

        // Pega as views do layout do dialog
        val tvSerial = dialogView.findViewById<TextView>(R.id.tvSerial)
        val tvAcquirer = dialogView.findViewById<TextView>(R.id.tvAcquirer)
        val tvModel = dialogView.findViewById<TextView>(R.id.tvModel)
        val btnOk = dialogView.findViewById<android.widget.Button>(R.id.btnOk)

        // Obtém os valores do DeviceUtils
        val model = br.com.ticpass.pos.core.util.DeviceUtils.getDeviceModel()
        val acquirer = br.com.ticpass.pos.core.util.DeviceUtils.getAcquirer()
        val serial = try {
            br.com.ticpass.pos.core.util.DeviceUtils.getDeviceSerial(this)
        } catch (e: Exception) {
            "unknown"
        }

        // Preenche os TextViews
        tvModel.text = model
        tvAcquirer.text = acquirer
        tvSerial.text = serial

        // Constrói e mostra o AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun performEmailLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

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

    fun onLoadingFinished() {
        val frag = supportFragmentManager.findFragmentByTag("loading_fragment")
        if (frag != null) {
            supportFragmentManager.beginTransaction().remove(frag).commitAllowingStateLoss()
        }

        startActivity(Intent(this, LoginMenuActivity::class.java))
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
            loginErrorText.text = errorMsg
            loginErrorText.visibility = View.VISIBLE
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

    private fun startMenusPreloadAndProceed() {
        onLoadingFinished()
    }

    // Helpers para exibir/remover fragments de status (tag = "loading_fragment")
    private fun showFragment(fragment: Fragment) {
        // removemos anterior (se houver) e adicionamos o novo com a mesma tag
        removeLoadingFragmentIfExists()
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment, "loading_fragment")
            .commitAllowingStateLoss()
    }

    private fun removeLoadingFragmentIfExists() {
        val existing = supportFragmentManager.findFragmentByTag("loading_fragment")
        if (existing != null) {
            supportFragmentManager.beginTransaction().remove(existing).commitAllowingStateLoss()
        }
    }
}