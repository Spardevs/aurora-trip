package br.com.ticpass.pos.data.network

import android.content.Context
import br.com.ticpass.pos.data.api.APIService
import br.com.ticpass.pos.data.api.RefreshTokenRequest
import com.auth0.android.jwt.JWT
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
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

    private val apiService: APIService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.ticpass.com.br/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(APIService::class.java)
    }

    fun getValidToken(): String {
        val expirationStr = prefs.getString("token_expiration", null)
        val refreshToken = prefs.getString("refresh_token", null)

        expirationStr?.let {
            val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT' yyyy", Locale.ENGLISH)
            val expirationDate = sdf.parse(it)
            if (expirationDate != null && expirationDate.before(Date()) && !refreshToken.isNullOrBlank()) {
                runBlocking {
                    try {
                        val newTokenResponse = apiService.refreshToken(
                            RefreshTokenRequest(refresh = refreshToken)
                        )
                        val tokenExpiration = JWT(newTokenResponse.result.token).getClaim("exp").asDate()
                        prefs.edit().apply {
                            putString("auth_token", newTokenResponse.result.token)
                            putString("refresh_token", newTokenResponse.result.tokenRefresh)
                            putString("token_expiration", tokenExpiration.toString())
                            apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        return prefs.getString("auth_token", "") ?: ""
    }
}
