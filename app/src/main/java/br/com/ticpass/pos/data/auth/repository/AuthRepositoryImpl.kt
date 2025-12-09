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

    suspend fun signIn(email: String, password: String): Response<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = Base64.encodeToString("$email:$password".toByteArray(), Base64.NO_WRAP)
                // Enviar um JSON vazio válido (não string vazia)
                val requestBody = "{}".toRequestBody("application/json".toMediaType())

                val response = authService.signIn(
                    requestBody = requestBody,
                    authHeader = "Basic $credentials"
                )

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