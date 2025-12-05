package br.com.ticpass.pos.core.sdk.nfc

import android.content.Context

/**
 * Gertec proprietary NFC Provider (NO-OP)
 * 
 * This variant does not support acquirer-based NFC operations.
 * This is a stub to maintain consistency with other source sets.
 */
object NFCProvider : BaseNFCProvider<Unit> {
    
    override fun isInitialized(): Boolean = false
    
    override fun initialize(appContext: Context) {
        // NO-OP: NFC not supported in proprietary variant
    }
    
    override fun getInstance(): Unit {
        throw UnsupportedOperationException(
            "NFC provider not supported in proprietary Gertec variant. " +
            "This device only supports printing operations via acquirer SDK."
        )
    }
}
