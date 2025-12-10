package br.com.ticpass.pos.presentation.login.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    private lateinit var identifierEditText: EditText  // Email or Username
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var qrLoginButton: Button
    private lateinit var loginErrorText: TextView
    private lateinit var choiceContainer: View
    private lateinit var formContainer: View
    private lateinit var emailChoiceButton: Button
    private lateinit var buttonBack: MaterialButton
    private lateinit var fallingImg: ImageView
    private lateinit var passwordToggle: ImageButton
    private lateinit var loadingOverlay: View
    private var isPasswordVisible = false
    private var isLoginInProgress = false
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

        identifierEditText = findViewById(R.id.editTextTextEmailAddress)
        passwordEditText = findViewById(R.id.editTextTextPassword)
        passwordToggle = findViewById(R.id.passwordToggle)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loginButton = findViewById(R.id.button_confirm)
        qrLoginButton = findViewById(R.id.qr_code_login_button)
        loginErrorText = findViewById(R.id.login_error_text)

        // Password visibility toggle
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_visibility_off)
            }
            // Keep cursor at end
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        // Handle keyboard done/checkmark action
        passwordEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                attemptLogin()
                true
            } else {
                false
            }
        }

        // TextWatcher: limpa erros quando usuário digita
        val clearErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (loginErrorText.visibility == View.VISIBLE) {
                    loginErrorText.visibility = View.GONE
                    resetFieldState(identifierEditText)
                    resetFieldState(passwordEditText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        identifierEditText.addTextChangedListener(clearErrorWatcher)
        passwordEditText.addTextChangedListener(clearErrorWatcher)

        emailChoiceButton.setOnClickListener {
            choiceContainer.visibility = View.GONE
            formContainer.visibility = View.VISIBLE
            animateFalling(raise = true)
            identifierEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(identifierEditText, InputMethodManager.SHOW_IMPLICIT)
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
            attemptLogin()
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

    /**
     * Sealed class representing the type of login credential
     */
    private sealed class LoginCredential {
        data class Email(val email: String, val password: String) : LoginCredential()
        data class Username(val username: String, val password: String) : LoginCredential()
    }

    /**
     * Check if input looks like an email (contains @)
     */
    private fun isEmailFormat(input: String): Boolean {
        return input.contains("@")
    }

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate username format (alphanumeric, min 3 chars)
     */
    private fun isValidUsername(username: String): Boolean {
        // Username must be alphanumeric (letters, numbers, underscore allowed), min 3 chars
        val usernamePattern = Regex("^[a-zA-Z0-9_]{3,}$")
        return usernamePattern.matches(username)
    }

    /**
     * Validate inputs and return LoginCredential if valid, null otherwise
     */
    private fun validateInputs(): LoginCredential? {
        val identifier = identifierEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        // Check if identifier is empty
        if (identifier.isEmpty()) {
            showInputError("E-mail ou usuário obrigatório", identifierEditText)
            return null
        }

        // Determine if it's email or username based on @ presence
        val isEmail = isEmailFormat(identifier)

        if (isEmail) {
            // Validate as email
            if (!isValidEmail(identifier)) {
                showInputError("E-mail inválido. Verifique o formato (ex: usuario@email.com)", identifierEditText)
                return null
            }
        } else {
            // Validate as username
            if (!isValidUsername(identifier)) {
                showInputError("Usuário inválido. Use apenas letras, números e _ (mín. 3 caracteres)", identifierEditText)
                return null
            }
        }

        // Validate password
        if (password.isEmpty()) {
            showInputError("Senha obrigatória", passwordEditText)
            return null
        }

        if (password.length < 12) {
            showInputError("Senha deve ter no mínimo 12 caracteres", passwordEditText)
            return null
        }

        return if (isEmail) {
            LoginCredential.Email(identifier, password)
        } else {
            LoginCredential.Username(identifier, password)
        }
    }

    /**
     * Hide the soft keyboard
     */
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Show loading overlay and disable login button
     */
    private fun showLoading() {
        isLoginInProgress = true
        loginButton.isEnabled = false
        loadingOverlay.visibility = View.VISIBLE
    }

    /**
     * Hide loading overlay and re-enable login button
     */
    private fun hideLoading() {
        isLoginInProgress = false
        loginButton.isEnabled = true
        loadingOverlay.visibility = View.GONE
    }

    /**
     * Attempt login - validates inputs, hides keyboard, and performs login
     */
    private fun attemptLogin() {
        if (isLoginInProgress) return
        hideKeyboard()
        val validationResult = validateInputs()
        if (validationResult != null) {
            performLogin(validationResult)
        }
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
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

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

    private fun performLogin(credential: LoginCredential) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            Timber.tag("LoginActivity").e(throwable, "Exception no handler (login)")
            runOnUiThread {
                hideLoading()
                Toast.makeText(this, "Erro de conexão: ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        }

        showLoading()

        lifecycleScope.launch(handler) {
            try {
                val loginType = when (credential) {
                    is LoginCredential.Email -> "e-mail"
                    is LoginCredential.Username -> "usuário"
                }
                Timber.tag("LoginActivity").d("Iniciando login por $loginType")

                val response: Response<LoginResponse> = withContext(Dispatchers.IO) {
                    when (credential) {
                        is LoginCredential.Email -> authRepository.signInWithEmail(credential.email, credential.password)
                        is LoginCredential.Username -> authRepository.signInWithUsername(credential.username, credential.password)
                    }
                }

                Timber.tag("LoginActivity").d("HTTP=${response.code()} success=${response.isSuccessful}")

                if (response.isSuccessful && (response.code() == 200 || response.code() == 201)) {
                    val body = response.body()
                    if (body == null) {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            Toast.makeText(this@LoginActivity, "Resposta vazia do servidor", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    // Token already saved by AuthRepositoryImpl.handleLoginResponse via TokenManager
                    Timber.tag("LoginActivity").d("Dados de autenticação salvos via TokenManager")
                    
                    // Navigate to next screen
                    withContext(Dispatchers.Main) {
                        startMenusPreloadAndProceed()
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                    }
                    handleErrorResponse(response, credential)
                }
            } catch (e: Exception) {
                Timber.tag("LoginActivity").e(e, "Erro no login")
                withContext(Dispatchers.Main) {
                    hideLoading()
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

    private suspend fun handleErrorResponse(response: Response<LoginResponse>, credential: LoginCredential) {
        val credentialType = when (credential) {
            is LoginCredential.Email -> "E-mail"
            is LoginCredential.Username -> "Usuário"
        }
        
        val errorMsg = when (response.code()) {
            400 -> "Requisição inválida"
            401 -> "$credentialType ou senha incorretos"
            404 -> "$credentialType não encontrado"
            in 500..599 -> "Erro no servidor"
            else -> "Erro desconhecido (status ${response.code()})"
        }

        withContext(Dispatchers.Main) {
            loginErrorText.text = errorMsg
            loginErrorText.visibility = View.VISIBLE
        }
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