package br.com.ticpass.pos.core.network.interceptor

import br.com.ticpass.pos.core.network.ratelimit.RateLimiter
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimitInterceptor @Inject constructor(
    private val rateLimiter: RateLimiter
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val key = "rate_limit_${request.url.host}${request.url.encodedPath}"

        return try {
            // Verifica e aplica rate limiting
            runBlocking { rateLimiter.checkRateLimit(key) }

            // Executa a requisição
            val response = chain.proceed(request)

            // Atualiza a política com base nos headers da resposta
            runBlocking { rateLimiter.updatePolicyFromHeaders(key, response.headers) }

            // Trata erro 429 (rate limit exceeded)
            if (response.code == 429) {
                Timber.tag("RateLimitInterceptor").w("Rate limit exceeded for $key")
                response.close() // Fecha a response antes de tentar novamente

                val shouldRetry = runBlocking { rateLimiter.handleRateLimitError(key) }
                if (shouldRetry) {
                    // Recursivamente tenta novamente após o backoff
                    return intercept(chain)
                } else {
                    runBlocking { rateLimiter.resetAttempts(key) }
                    // Se não podemos mais tentar, retornamos a response de erro
                    return response
                }
            } else {
                // Resetamos as tentativas se a requisição foi bem sucedida
                runBlocking { rateLimiter.resetAttempts(key) }
            }

            response
        } catch (e: Exception) {
            Timber.tag("RateLimitInterceptor").e(e, "Error in rate limit interceptor")
            throw e
        }
    }
}