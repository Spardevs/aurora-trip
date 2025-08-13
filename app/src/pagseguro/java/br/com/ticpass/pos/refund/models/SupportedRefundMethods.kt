package br.com.ticpass.pos.refund.models

/**
 * PagSeguro supported refund methods
 */
object SupportedRefundMethods {
    val methods = listOf(
        SystemRefundMethod.ACQUIRER,
    )
}