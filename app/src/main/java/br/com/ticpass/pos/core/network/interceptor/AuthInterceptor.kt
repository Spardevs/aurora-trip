package br.com.ticpass.pos.core.network.interceptor

import br.com.ticpass.pos.core.network.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessTokenSync()
        Timber.tag("AuthInterceptor")
            .d("intercept called, token present=${!accessToken.isNullOrEmpty()}")
        if (accessToken.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Cookie", "access=$accessToken")
            .build()
        return chain.proceed(authenticatedRequest)
    }
}