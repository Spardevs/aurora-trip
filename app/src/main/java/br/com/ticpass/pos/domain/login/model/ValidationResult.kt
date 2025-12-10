package br.com.ticpass.pos.domain.login.model

/**
 * Result of credential validation
 */
sealed class ValidationResult {
    data class Valid(val credential: LoginCredential) : ValidationResult()
    data class Invalid(val error: ValidationError) : ValidationResult()
}

/**
 * Types of validation errors
 */
enum class ValidationError {
    EMPTY_IDENTIFIER,
    INVALID_EMAIL_FORMAT,
    INVALID_USERNAME_FORMAT,
    EMPTY_PASSWORD,
    PASSWORD_TOO_SHORT
}
