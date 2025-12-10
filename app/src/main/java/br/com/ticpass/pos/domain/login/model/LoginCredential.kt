package br.com.ticpass.pos.domain.login.model

/**
 * Sealed class representing the type of login credential
 */
sealed class LoginCredential {
    data class Email(val email: String, val password: String) : LoginCredential()
    data class Username(val username: String, val password: String) : LoginCredential()
}
