package br.com.ticpass.pos.sdk.refund

import android.content.Context
import br.com.ticpass.pos.sdk.SdkInstance
import br.com.ticpass.pos.sdk.factory.RefundProvider as RefundProviderType
import br.com.ticpass.pos.sdk.factory.RefundProviderFactory

/**
 * Gertec SK210 proprietary Refund Provider (NO-OP)
 * 
 * This variant does not support acquirer-based refund operations.
 * This is a stub to maintain consistency with other source sets.
 */
object RefundProvider : BaseRefundProvider<RefundProviderType> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): RefundProviderType {
        if (!isInitialized()) {
            throw IllegalStateException("Refund provider not initialized. Call initialize() first.")
        }
        val context = SdkInstance.getInstance()
        val refundFactory = RefundProviderFactory(context)
        return refundFactory.create()
    }
}
