package br.com.ticpass.pos.data.network

import android.content.Context
import android.util.Log
import br.com.ticpass.Constants
import br.com.ticpass.pos.data.api.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("${Constants.API_HOST}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun getAccessToken(): String {
        return prefs.getString("auth_token", "") ?: ""
    }

    fun getRefreshToken(): String {
        return prefs.getString("refresh_token", "") ?: ""
    }

    fun getProxyCredentials(): String {
        return prefs.getString("proxy_credentials", "") ?: ""
    }

    fun isTokenExpired(): Boolean {
        val expirationStr = prefs.getString("access_token_expiration", null) ?: return true
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val expirationDate = sdf.parse(expirationStr)
            expirationDate?.before(Date()) ?: true
        } catch (e: Exception) {
            Log.e("TokenManager", "Error parsing token expiration", e)
            true
        }
    }

    suspend fun refreshTokenIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (!isTokenExpired()) {
            return@withContext true
        }

        val refreshToken = getRefreshToken()
        val accessToken = getAccessToken()
        val proxyCredentials = getProxyCredentials()

        if (refreshToken.isEmpty() || proxyCredentials.isEmpty()) {
            Log.e("TokenManager", "Missing refresh token or proxy credentials")
            return@withContext false
        }

        return@withContext try {
            // Monta o Cookie header: refresh=<token>;access=<token>
            val cookieHeader = "refresh=$refreshToken;access=$accessToken"

            val response = apiService.refreshToken(
                cookie = cookieHeader,
                authorization = proxyCredentials
            )

            if (response.isSuccessful && response.body() != null) {
                val refreshResponse = response.body()!!

                // Salva os novos tokens
                prefs.edit().apply {
                    putString("access_token_expiration", refreshResponse.jwt.access)
                    putString("refresh_token_expiration", refreshResponse.jwt.refresh)
                    // Note: O backend retorna as datas de expiração, não os tokens em si
                    // Os tokens continuam nos cookies
                    apply()
                }

                Log.d("TokenManager", "Token refreshed successfully")
                true
            } else {
                Log.e("TokenManager", "Failed to refresh token: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error refreshing token", e)
            false
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String, accessExpiration: String, refreshExpiration: String) {
        prefs.edit().apply {
            putString("auth_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("access_token_expiration", accessExpiration)
            putString("refresh_token_expiration", refreshExpiration)
            apply()
        }
    }

    fun saveProxyCredentials(credentials: String) {
        prefs.edit().apply {
            putString("proxy_credentials", credentials)
            apply()
        }
    }

    fun clearTokens() {
        prefs.edit().apply {
            remove("auth_token")
            remove("refresh_token")
            remove("access_token_expiration")
            remove("refresh_token_expiration")
            remove("proxy_credentials")
            apply()
        }
    }
}

class TokenRefreshException(message: String, cause: Throwable?) : Exception(message, cause)