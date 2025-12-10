package br.com.ticpass.pos.data.auth.repository

import android.util.Base64
import br.com.ticpass.pos.data.auth.remote.service.AuthService
import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.core.network.TokenManager
import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val tokenManager: TokenManager,
    private val database: AppDatabase
) {

    /**
     * Sign in with email and password
     * Body: {"email": "<email>", "password": "<password>"}
     */
    suspend fun signInWithEmail(email: String, password: String): Response<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = """{"email": "$email", "password": "$password"}"""
                    .toRequestBody("application/json"
                    .toMediaType())
                val response = authService.signIn(requestBody)

                handleLoginResponse(response)
                response
            } catch (e: Exception) {
                throw IOException("Erro de rede", e)
            }
        }
    }

    /**
     * Sign in with username and password
     * Body: {"username": "<username>", "password": "<password>"}
     */
    suspend fun signInWithUsername(username: String, password: String): Response<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = """{"username": "$username", "password": "$password"}"""
                    .toRequestBody("application/json"
                    .toMediaType())

                val response = authService.signIn(requestBody)

                handleLoginResponse(response)
                response
            } catch (e: Exception) {
                throw IOException("Erro de rede", e)
            }
        }
    }

    private suspend fun handleLoginResponse(response: Response<LoginResponse>) {
        if (response.isSuccessful) {
            response.body()?.let { loginResponse ->
                val cookies = response.headers().values("Set-Cookie")
                val accessCookie = cookies.firstOrNull { it.startsWith("access=") }
                val refreshCookie = cookies.firstOrNull { it.startsWith("refresh=") }

                val accessToken = accessCookie
                    ?.substringAfter("access=")
                    ?.substringBefore(";")
                    ?.takeIf { it.isNotBlank() }
                    ?: loginResponse.jwt.access

                val refreshToken = refreshCookie
                    ?.substringAfter("refresh=")
                    ?.substringBefore(";")
                    ?.takeIf { it.isNotBlank() }
                    ?: loginResponse.jwt.refresh

                tokenManager.saveTokens(accessToken, refreshToken)
            }
        }
    }

    suspend fun signInWithQrCode(qrToken: String, payloadJson: String = "{}"): Response<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody: RequestBody = payloadJson.toRequestBody(mediaType)

                // Header Cookie exatamente como no curl
                val cookieHeader = "shortLived=$qrToken"

                val response = authService.signInShortLived(requestBody, cookieHeader)

                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        val cookies = response.headers().values("Set-Cookie")
                        val accessCookie = cookies.firstOrNull { it.startsWith("access=") }
                        val refreshCookie = cookies.firstOrNull { it.startsWith("refresh=") }

                        val accessToken = accessCookie
                            ?.substringAfter("access=")
                            ?.substringBefore(";")
                            ?.takeIf { it.isNotBlank() }
                            ?: loginResponse.jwt.access

                        val refreshToken = refreshCookie
                            ?.substringAfter("refresh=")
                            ?.substringBefore(";")
                            ?.takeIf { it.isNotBlank() }
                            ?: loginResponse.jwt.refresh

                        tokenManager.saveTokens(accessToken, refreshToken)
                    }
                }

                response
            } catch (e: Exception) {
                throw IOException("Erro de rede", e)
            }
        }
    }

    suspend fun getStoredAccessToken(): String? {
        return tokenManager.getAccessToken()
    }
}