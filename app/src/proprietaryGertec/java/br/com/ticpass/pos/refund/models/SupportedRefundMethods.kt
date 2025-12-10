package br.com.ticpass.pos.refund.models

import br.com.ticpass.pos.core.refund.models.SystemRefundMethod

/**
 * Gertec proprietary supported refund methods
 * 
 * NO-OP: Proprietary variant does not support acquirer-based refund operations.
 * This is a stub to maintain consistency with other source sets.
 */
object SupportedRefundMethods {
    val methods = emptyList<SystemRefundMethod>()
}
