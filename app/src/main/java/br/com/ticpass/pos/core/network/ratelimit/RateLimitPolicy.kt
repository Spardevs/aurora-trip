package br.com.ticpass.pos.core.network.ratelimit

import kotlinx.serialization.Serializable

@Serializable
data class RateLimitPolicy(
    val name: String,
    val limit: Int,
    val remaining: Int,
    val reset: Long, // Timestamp Unix em ms
    val lastUpdated: Long
)