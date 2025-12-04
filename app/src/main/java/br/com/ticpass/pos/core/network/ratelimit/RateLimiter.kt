package br.com.ticpass.pos.core.network.ratelimit

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val storage: RateLimitStorage,
    private val backoff: ExponentialBackoff
) {
    private val mutex = Mutex()
    private val requestAttempts = ConcurrentHashMap<String, Int>()

    suspend fun checkRateLimit(key: String) {
        mutex.withLock {
            val policy = storage.getPolicy(key)
            if (policy != null) {
                val now = System.currentTimeMillis()

                // Se o tempo de reset já passou, resetamos o contador
                if (now >= policy.reset) {
                    val updatedPolicy = policy.copy(
                        remaining = policy.limit,
                        lastUpdated = now
                    )
                    storage.savePolicy(key, updatedPolicy)
                    return@withLock
                }

                // Se não temos requests restantes, esperamos até o reset
                if (policy.remaining <= 0) {
                    val waitTime = policy.reset - now
                    if (waitTime > 0) {
                        Timber.tag("RateLimiter").d("Waiting $waitTime ms for rate limit reset")
                        delay(waitTime)
                    }

                    // Após esperar, resetamos o contador
                    val updatedPolicy = policy.copy(
                        remaining = policy.limit,
                        lastUpdated = System.currentTimeMillis()
                    )
                    storage.savePolicy(key, updatedPolicy)
                } else {
                    // Decrementamos o contador de requests restantes
                    val updatedPolicy = policy.copy(
                        remaining = policy.remaining - 1,
                        lastUpdated = now
                    )
                    storage.savePolicy(key, updatedPolicy)
                }
            }
        }
    }

    suspend fun updatePolicyFromHeaders(key: String, headers: okhttp3.Headers) {
        try {
            val limit = headers["X-RateLimit-Limit"]?.toIntOrNull()
            val remaining = headers["X-RateLimit-Remaining"]?.toIntOrNull()
            val reset = headers["X-RateLimit-Reset"]?.toLongOrNull()
            val policyName = headers["X-RateLimit-Policy"] ?: "default"

            if (limit != null && remaining != null && reset != null) {
                val policy = RateLimitPolicy(
                    name = policyName,
                    limit = limit,
                    remaining = remaining,
                    reset = reset * 1000, // Converter para milliseconds
                    lastUpdated = System.currentTimeMillis()
                )
                storage.savePolicy(key, policy)
                Timber.tag("RateLimiter").d("Updated rate limit policy: $policy")
            }
        } catch (e: Exception) {
            Timber.tag("RateLimiter").e(e, "Failed to parse rate limit headers")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun handleRateLimitError(key: String): Boolean {
        val attempts = requestAttempts.getOrDefault(key, 0) + 1
        requestAttempts[key] = attempts

        if (backoff.isMaxRetriesReached(attempts)) {
            requestAttempts.remove(key)
            return false
        }

        val policy = storage.getPolicy(key)
        if (policy != null) {
            val delayMs = backoff.calculateDelay(attempts, 1000)
            Timber.tag("RateLimiter").d("Rate limit exceeded. Retrying in $delayMs ms (attempt $attempts)")
            delay(delayMs)
            return true
        }

        // Se não temos política, esperamos um tempo padrão
        val delayMs = backoff.calculateDelay(attempts, 1000)
        delay(delayMs)
        return true
    }

    fun resetAttempts(key: String) {
        requestAttempts.remove(key)
    }
}