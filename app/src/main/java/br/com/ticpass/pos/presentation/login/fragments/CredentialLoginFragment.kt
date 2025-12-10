package br.com.ticpass.pos.presentation.login.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.domain.login.model.ValidationError
import br.com.ticpass.pos.presentation.login.activities.LoginMenuActivity
import br.com.ticpass.pos.presentation.login.states.LoginUiState
import br.com.ticpass.pos.presentation.login.viewmodels.LoginViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for credential-based login (email/username + password)
 */
@AndroidEntryPoint
class CredentialLoginFragment : Fragment(R.layout.fragment_credential_login) {

    private val loginViewModel: LoginViewModel by viewModels()

    // Views
    private lateinit var identifierEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordToggle: ImageButton
    private lateinit var loginButton: Button
    private lateinit var backButton: MaterialButton
    private lateinit var errorText: TextView
    private lateinit var loadingOverlay: View

    private var isPasswordVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        observeLoginState()
        
        // Focus on identifier field
        identifierEditText.requestFocus()
        showKeyboard(identifierEditText)
    }

    private fun initViews(view: View) {
        identifierEditText = view.findViewById(R.id.editTextTextEmailAddress)
        passwordEditText = view.findViewById(R.id.editTextTextPassword)
        passwordToggle = view.findViewById(R.id.passwordToggle)
        loginButton = view.findViewById(R.id.button_confirm)
        backButton = view.findViewById(R.id.button_back)
        errorText = view.findViewById(R.id.login_error_text)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
    }

    private fun setupListeners() {
        // Password visibility toggle
        passwordToggle.setOnClickListener { togglePasswordVisibility() }

        // Keyboard done action
        passwordEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                attemptLogin()
                true
            } else false
        }

        // Clear errors when typing
        val clearErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loginViewModel.clearValidationError()
                if (errorText.visibility == View.VISIBLE) {
                    errorText.visibility = View.GONE
                    resetFieldState(identifierEditText)
                    resetFieldState(passwordEditText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        identifierEditText.addTextChangedListener(clearErrorWatcher)
        passwordEditText.addTextChangedListener(clearErrorWatcher)

        // Back button
        backButton.setOnClickListener {
            hideKeyboard()
            (activity as? LoginHostCallback)?.onHideCredentialForm()
            parentFragmentManager.popBackStack()
        }

        // Login button
        loginButton.setOnClickListener { attemptLogin() }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> hideLoading()
                    is LoginUiState.Loading -> showLoading()
                    is LoginUiState.Success -> navigateToMenuActivity()
                    is LoginUiState.ValidationFailed -> {
                        hideLoading()
                        handleValidationError(state.error)
                    }
                    is LoginUiState.Error -> {
                        hideLoading()
                        showError(state.exception.getLocalizedMessage(requireContext()))
                    }
                }
            }
        }
    }

    private fun attemptLogin() {
        hideKeyboard()
        loginViewModel.login(
            identifier = identifierEditText.text.toString(),
            password = passwordEditText.text.toString()
        )
    }

    private fun handleValidationError(error: ValidationError) {
        val (messageResId, field) = when (error) {
            ValidationError.EMPTY_IDENTIFIER -> R.string.error_empty_identifier to identifierEditText
            ValidationError.INVALID_EMAIL_FORMAT -> R.string.error_invalid_email to identifierEditText
            ValidationError.INVALID_USERNAME_FORMAT -> R.string.error_invalid_username to identifierEditText
            ValidationError.EMPTY_PASSWORD -> R.string.error_empty_password to passwordEditText
            ValidationError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short to passwordEditText
        }
        showInputError(getString(messageResId), field)
    }

    private fun showInputError(message: String, field: EditText) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        field.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        field.background?.let { 
            DrawableCompat.setTint(it, ContextCompat.getColor(requireContext(), R.color.colorRedAccent)) 
        }
        field.requestFocus()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun resetFieldState(field: EditText) {
        try {
            field.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            field.background?.let { 
                DrawableCompat.setTint(it, ContextCompat.getColor(requireContext(), R.color.colorWhite)) 
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun showLoading() {
        loginButton.isEnabled = false
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loginButton.isEnabled = true
        loadingOverlay.visibility = View.GONE
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            passwordToggle.setImageResource(R.drawable.ic_visibility_on)
        } else {
            passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            passwordToggle.setImageResource(R.drawable.ic_visibility_off)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        activity?.currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun navigateToMenuActivity() {
        startActivity(Intent(requireContext(), LoginMenuActivity::class.java))
        activity?.finish()
    }
}
