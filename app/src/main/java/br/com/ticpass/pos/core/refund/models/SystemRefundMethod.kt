package br.com.ticpass.pos.core.refund.models

enum class SystemRefundMethod(
    val value: String,
) {
    ACQUIRER("acquirer");

    companion object {
        fun fromValue(value: String): SystemRefundMethod {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown refund method: $value")
        }
    }
}