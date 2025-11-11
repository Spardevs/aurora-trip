package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.pos.sdk.factory.AcquirerPrintingProvider
import br.com.ticpass.pos.sdk.nfc.BaseNFCProvider
import br.com.ticpass.pos.sdk.nfc.NFCProvider
import br.com.ticpass.pos.sdk.payment.BasePaymentProvider
import br.com.ticpass.pos.sdk.payment.PaymentProvider
import br.com.ticpass.pos.sdk.printing.BasePrintingProvider
import br.com.ticpass.pos.sdk.printing.PrintingProvider
import br.com.ticpass.pos.sdk.refund.BaseRefundProvider
import br.com.ticpass.pos.sdk.refund.RefundProvider

/**
 * Central access point for acquirer SDK instances
 *
 * This class provides direct access to acquirer provider instances for Gertec.
 * 
 * Note: Only printing is supported in this variant. Payment, NFC, and Refund providers
 * are no-op stubs that throw UnsupportedOperationException.
 */
object AcquirerSdk {
    /**
     * Gertec printing provider instance (SUPPORTED)
     */
    val printing: BasePrintingProvider<AcquirerPrintingProvider>
        get() = PrintingProvider

    /**
     * Payment provider (NO-OP STUB)
     * Throws UnsupportedOperationException when getInstance() is called
     */
    val payment: BasePaymentProvider<Unit>
        get() = PaymentProvider

    /**
     * NFC provider (NO-OP STUB)
     * Throws UnsupportedOperationException when getInstance() is called
     */
    val nfc: BaseNFCProvider<Unit>
        get() = NFCProvider

    /**
     * Refund provider (NO-OP STUB)
     * Throws UnsupportedOperationException when getInstance() is called
     */
    val refund: BaseRefundProvider<br.com.ticpass.pos.sdk.factory.RefundProvider>
        get() = RefundProvider

    /**
     * Initialize all providers with their required parameters
     *
     * @param appContext The application context
     */
    fun initialize(appContext: Context) {
        // Initialize only printing provider (others are no-op stubs)
        printing.initialize(appContext)
        
        // No-op providers don't need initialization
        // payment.initialize(appContext) - no-op
        // nfc.initialize(appContext) - no-op
        // refund.initialize(appContext) - no-op
    }

    /**
     * Get the Stone Code (not applicable for Proprietary Gertec)
     *
     * @return Empty string for Proprietary Gertec flavor
     */
    fun getStoneCode(): String {
        return ""
    }

    /**
     * Get the device serial (not applicable for Proprietary Gertec)
     *
     * @return null for Proprietary Gertec flavor (use DeviceUtils.getDeviceSerial instead)
     */
    fun getDeviceSerial(): String? {
        return null
    }

    /**
     * Check if the SDK is initialized
     *
     * @return Always returns true for Proprietary Gertec (no SDK initialization required)
     */
    fun isInitialized(): Boolean {
        return true
    }
}
