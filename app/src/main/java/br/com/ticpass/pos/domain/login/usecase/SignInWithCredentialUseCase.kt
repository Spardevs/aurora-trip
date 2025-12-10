package br.com.ticpass.pos.domain.login.usecase

import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse
import br.com.ticpass.pos.data.auth.repository.AuthRepositoryImpl
import br.com.ticpass.pos.domain.login.model.LoginCredential
import javax.inject.Inject

/**
 * Use case for signing in with email or username credentials
 */
class SignInWithCredentialUseCase @Inject constructor(
    private val authRepository: AuthRepositoryImpl
) {
    /**
     * Execute sign in with the provided credential
     * @return Result containing LoginResponse on success or Exception on failure
     */
    suspend operator fun invoke(credential: LoginCredential): Result<LoginResponse> {
        return try {
            val response = when (credential) {
                is LoginCredential.Email -> 
                    authRepository.signInWithEmail(credential.email, credential.password)
                is LoginCredential.Username -> 
                    authRepository.signInWithUsername(credential.username, credential.password)
            }

            if (response.isSuccessful && (response.code() == 200 || response.code() == 201)) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(LoginException.EmptyResponse)
                }
            } else {
                Result.failure(LoginException.fromHttpCode(response.code(), credential))
            }
        } catch (e: Exception) {
            Result.failure(LoginException.NetworkError(e.message))
        }
    }
}

/**
 * Login-specific exceptions with string resource IDs for user-facing messages.
 * Use [getLocalizedMessage] with a Context to get the translated message.
 */
sealed class LoginException(
    val messageResId: Int,
    val formatArgs: Array<out Any> = emptyArray()
) : Exception() {
    
    object EmptyResponse : LoginException(br.com.ticpass.pos.R.string.error_empty_response)
    
    data class InvalidCredentials(val credentialTypeResId: Int) : LoginException(
        br.com.ticpass.pos.R.string.error_invalid_credentials,
        arrayOf(credentialTypeResId)
    ) {
        override fun getLocalizedMessage(context: android.content.Context): String {
            val credentialType = context.getString(credentialTypeResId)
            return context.getString(messageResId, credentialType)
        }
    }
    
    data class NotFound(val credentialTypeResId: Int) : LoginException(
        br.com.ticpass.pos.R.string.error_not_found,
        arrayOf(credentialTypeResId)
    ) {
        override fun getLocalizedMessage(context: android.content.Context): String {
            val credentialType = context.getString(credentialTypeResId)
            return context.getString(messageResId, credentialType)
        }
    }
    
    object BadRequest : LoginException(br.com.ticpass.pos.R.string.error_bad_request)
    object ServerError : LoginException(br.com.ticpass.pos.R.string.error_server)
    
    data class Unknown(val code: Int) : LoginException(
        br.com.ticpass.pos.R.string.error_unknown,
        arrayOf(code)
    )
    
    data class NetworkError(val details: String?) : LoginException(
        br.com.ticpass.pos.R.string.error_network,
        arrayOf(details ?: "")
    )

    /**
     * Get localized message using Context
     */
    open fun getLocalizedMessage(context: android.content.Context): String {
        return if (formatArgs.isEmpty()) {
            context.getString(messageResId)
        } else {
            context.getString(messageResId, *formatArgs)
        }
    }

    companion object {
        fun fromHttpCode(code: Int, credential: LoginCredential): LoginException {
            val credentialTypeResId = when (credential) {
                is LoginCredential.Email -> br.com.ticpass.pos.R.string.credential_type_email
                is LoginCredential.Username -> br.com.ticpass.pos.R.string.credential_type_username
            }
            return when (code) {
                400 -> BadRequest
                401 -> InvalidCredentials(credentialTypeResId)
                404 -> NotFound(credentialTypeResId)
                in 500..599 -> ServerError
                else -> Unknown(code)
            }
        }
    }
}
