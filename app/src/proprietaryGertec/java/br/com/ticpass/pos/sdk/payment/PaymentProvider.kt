package br.com.ticpass.pos.sdk.payment

import android.content.Context

/**
 * Gertec proprietary Payment Provider (NO-OP)
 * 
 * This variant does not support acquirer-based payment processing.
 * This is a stub to maintain consistency with other source sets.
 */
object PaymentProvider : BasePaymentProvider<Unit> {
    
    override fun isInitialized(): Boolean = false
    
    override fun initialize(appContext: Context) {
        // NO-OP: Payment not supported in proprietary variant
    }
    
    override fun getInstance(): Unit {
        throw UnsupportedOperationException(
            "Payment provider not supported in proprietary Gertec variant. " +
            "This device only supports printing operations via acquirer SDK."
        )
    }
}
