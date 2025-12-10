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
        val original = chain.request()
        val path = original.url.encodedPath

        // 1) Se o request já traz Cookie, respeita (ex: você montou o request com Cookie shortLived)
        if (!original.header("Cookie").isNullOrBlank()) {
            Timber.tag("AuthInterceptor").d("Request already has Cookie header, skipping injection for path=$path")
            return chain.proceed(original)
        }

        // 2) Skip cookie injection for all signin routes (user is authenticating, not authenticated)
        if (path.contains("/auth/signin")) {
            Timber.tag("AuthInterceptor").d("Skipping cookie injection for signin route: $path")
            return chain.proceed(original)
        }

        val accessToken = tokenManager.getAccessTokenSync()

        val cookieValue = when {
            !accessToken.isNullOrEmpty() -> "access=$accessToken"
            else -> null
        }

        return if (cookieValue == null) {
            chain.proceed(original)
        } else {
            val authenticatedRequest = original.newBuilder()
                .header("Cookie", cookieValue) // usa header para evitar duplicatas
                .build()
            chain.proceed(authenticatedRequest)
        }
    }
}