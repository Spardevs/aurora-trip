package br.com.ticpass.pos.presentation.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.login.model.ValidationResult
import br.com.ticpass.pos.domain.login.usecase.LoginException
import br.com.ticpass.pos.domain.login.usecase.SignInWithCredentialUseCase
import br.com.ticpass.pos.domain.login.validator.CredentialValidator
import br.com.ticpass.pos.presentation.login.states.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for credential-based login (email/username + password)
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithCredentialUseCase: SignInWithCredentialUseCase,
    private val credentialValidator: CredentialValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Attempt login with the provided identifier and password
     */
    fun login(identifier: String, password: String) {
        // Prevent multiple simultaneous login attempts
        if (_uiState.value is LoginUiState.Loading) return

        // Validate inputs
        when (val validationResult = credentialValidator.validate(identifier, password)) {
            is ValidationResult.Invalid -> {
                _uiState.value = LoginUiState.ValidationFailed(validationResult.error)
                return
            }
            is ValidationResult.Valid -> {
                performLogin(validationResult.credential)
            }
        }
    }

    private fun performLogin(credential: br.com.ticpass.pos.domain.login.model.LoginCredential) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val loginType = when (credential) {
                is br.com.ticpass.pos.domain.login.model.LoginCredential.Email -> "e-mail"
                is br.com.ticpass.pos.domain.login.model.LoginCredential.Username -> "usuário"
            }
            Timber.tag("LoginViewModel").d("Iniciando login por $loginType")

            val result = signInWithCredentialUseCase(credential)

            result.fold(
                onSuccess = {
                    Timber.tag("LoginViewModel").d("Login successful")
                    _uiState.value = LoginUiState.Success
                },
                onFailure = { exception ->
                    Timber.tag("LoginViewModel").e(exception, "Login failed")
                    val loginException = exception as? LoginException 
                        ?: LoginException.NetworkError(exception.message)
                    _uiState.value = LoginUiState.Error(loginException)
                }
            )
        }
    }

    /**
     * Reset state to idle (e.g., after showing error)
     */
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    /**
     * Clear validation error (e.g., when user starts typing)
     */
    fun clearValidationError() {
        if (_uiState.value is LoginUiState.ValidationFailed || _uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
