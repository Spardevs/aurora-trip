package br.com.ticpass.pos.data.network.interceptor

import br.com.ticpass.pos.data.network.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiAuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Tenta refresh se necessário (apenas para endpoints que não sejam auth)
        if (!original.url.encodedPath.contains("/auth/")) {
            runBlocking {
                try {
                    tokenManager.refreshTokenIfNeeded()
                } catch (e: Exception) {
                    // Log se quiser
                }
            }
        }

        val builder = original.newBuilder()

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val proxyCredentials = tokenManager.getProxyCredentials()

        val hasAuthHeader = !original.header("Authorization").isNullOrBlank()
        val hasCookieHeader = !original.header("Cookie").isNullOrBlank()

        // Só adiciona Authorization se não houver Authorization E não houver Cookie (evita conflito)
        if (!hasAuthHeader && !hasCookieHeader) {
            if (proxyCredentials.isNotEmpty()) {
                builder.header("Authorization", proxyCredentials)
            } else if (accessToken.isNotEmpty()) {
                builder.header("Authorization", "Bearer $accessToken")
            }
        }

        // Se não há Cookie definido explicitamente, adiciona cookie com refresh+access quando disponível
        if (!hasCookieHeader && accessToken.isNotEmpty()) {
            val cookieValue = if (refreshToken.isNotBlank()) {
                "refresh=$refreshToken;access=$accessToken"
            } else {
                "access=$accessToken"
            }
            builder.header("Cookie", cookieValue)
        }

        return chain.proceed(builder.build())
    }
}