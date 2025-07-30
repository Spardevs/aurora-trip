package br.com.ticpass.pos.data.network.interceptor

import br.com.ticpass.pos.data.network.TokenManager
import br.com.ticpass.pos.data.network.TokenRefreshException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // First try with current token
        val token = tokenManager.getValidToken()
        val initialRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        // Execute initial request
        val response = chain.proceed(initialRequest)

        if (response.code == 401) {
            response.close()
            try {
                val refreshed = runBlocking {
                    tokenManager.refreshTokenIfNeeded()
                }

                if (refreshed) {
                    val newToken = tokenManager.getValidToken()
                    val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $newToken")
                        .build()
                    return chain.proceed(newRequest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return response
    }
}
