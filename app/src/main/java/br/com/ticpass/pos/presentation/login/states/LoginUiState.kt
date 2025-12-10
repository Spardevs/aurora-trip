package br.com.ticpass.pos.presentation.login.states

import br.com.ticpass.pos.domain.login.model.ValidationError
import br.com.ticpass.pos.domain.login.usecase.LoginException

/**
 * UI state for the login screen
 */
sealed class LoginUiState {
    /** Initial idle state */
    object Idle : LoginUiState()
    
    /** Login in progress */
    object Loading : LoginUiState()
    
    /** Login successful - navigate to next screen */
    object Success : LoginUiState()
    
    /** Validation error - show field-specific error */
    data class ValidationFailed(val error: ValidationError) : LoginUiState()
    
    /** Login error - pass exception for localized message */
    data class Error(val exception: LoginException) : LoginUiState()
}
