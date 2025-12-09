package br.com.ticpass.pos.core.network.ratelimit

class ExponentialBackoff(
    private val maxRetries: Int = 5,
    private val maxWaitMs: Long = 60_000
) {
    fun calculateDelay(attempt: Int, baseWait: Long): Long {
        val exponentialWait = minOf(maxWaitMs, (1L shl attempt) * 1000)
        return baseWait + exponentialWait
    }

    fun isMaxRetriesReached(attempt: Int) = attempt > maxRetries
}