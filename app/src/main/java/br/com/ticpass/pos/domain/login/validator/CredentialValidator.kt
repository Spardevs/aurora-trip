package br.com.ticpass.pos.domain.login.validator

import android.util.Patterns
import br.com.ticpass.pos.domain.login.model.LoginCredential
import br.com.ticpass.pos.domain.login.model.ValidationError
import br.com.ticpass.pos.domain.login.model.ValidationResult
import javax.inject.Inject

/**
 * Validates login credentials (email/username and password)
 */
class CredentialValidator @Inject constructor() {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 12
        private val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]{3,}$")
    }

    /**
     * Validate identifier and password, returning a ValidationResult
     */
    fun validate(identifier: String, password: String): ValidationResult {
        val trimmedIdentifier = identifier.trim()

        // Check if identifier is empty
        if (trimmedIdentifier.isEmpty()) {
            return ValidationResult.Invalid(ValidationError.EMPTY_IDENTIFIER)
        }

        // Determine if it's email or username based on @ presence
        val isEmail = isEmailFormat(trimmedIdentifier)

        if (isEmail) {
            if (!isValidEmail(trimmedIdentifier)) {
                return ValidationResult.Invalid(ValidationError.INVALID_EMAIL_FORMAT)
            }
        } else {
            if (!isValidUsername(trimmedIdentifier)) {
                return ValidationResult.Invalid(ValidationError.INVALID_USERNAME_FORMAT)
            }
        }

        // Validate password
        if (password.isEmpty()) {
            return ValidationResult.Invalid(ValidationError.EMPTY_PASSWORD)
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            return ValidationResult.Invalid(ValidationError.PASSWORD_TOO_SHORT)
        }

        // All validations passed
        val credential = if (isEmail) {
            LoginCredential.Email(trimmedIdentifier, password)
        } else {
            LoginCredential.Username(trimmedIdentifier, password)
        }

        return ValidationResult.Valid(credential)
    }

    /**
     * Check if input looks like an email (contains @)
     */
    private fun isEmailFormat(input: String): Boolean {
        return input.contains("@")
    }

    /**
     * Validate email format using Android's Patterns
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate username format (alphanumeric + underscore, min 3 chars)
     */
    private fun isValidUsername(username: String): Boolean {
        return USERNAME_PATTERN.matches(username)
    }
}
