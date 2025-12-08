package br.com.ticpass.pos.refund.models

import br.com.ticpass.pos.core.refund.models.SystemRefundMethod

/**
 * PagSeguro supported refund methods
 */
object SupportedRefundMethods {
    val methods = listOf(
        SystemRefundMethod.ACQUIRER,
    )
}