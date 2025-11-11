package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.pos.sdk.nfc.BaseNFCProvider
import br.com.ticpass.pos.sdk.nfc.NFCProvider
import br.com.ticpass.pos.sdk.payment.BasePaymentProvider
import br.com.ticpass.pos.sdk.payment.PaymentProvider
import br.com.ticpass.pos.sdk.printing.BasePrintingProvider
import br.com.ticpass.pos.sdk.printing.PrintingProvider
import br.com.ticpass.pos.sdk.refund.BaseRefundProvider
import br.com.ticpass.pos.sdk.refund.RefundProvider
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag

/**
 * Central access point for acquirer SDK instances
 *
 * This class provides direct access to acquirer provider instances for the PagSeguro flavor.
 */
object AcquirerSdk {
    /**
     * PagSeguro payment provider instance
     */
    val payment: BasePaymentProvider<PlugPag>
        get() = PaymentProvider

    /**
     * PagSeguro printing provider instance
     */
    val printing: BasePrintingProvider<PlugPag>
        get() = PrintingProvider

    /**
     * PagSeguro NFC provider instance
     */
    val nfc: BaseNFCProvider<PlugPag>
        get() = NFCProvider

    /**
     * PagSeguro Refund provider instance
     */
    val refund: BaseRefundProvider<PlugPag>
        get() = RefundProvider

    /**
     * Initialize all providers with their required parameters
     *
     * @param appContext The application context
     */
    fun initialize(appContext: Context) {
        // Initialize all providers with application context
        payment.initialize(appContext)
        printing.initialize(appContext)
        nfc.initialize(appContext)
        refund.initialize(appContext)
    }

    /**
     * Get the Stone Code (not applicable for PagSeguro)
     *
     * @return Empty string for PagSeguro flavor
     */
    fun getStoneCode(): String {
        return ""
    }

    /**
     * Get the device serial from PagSeguro SDK
     *
     * @return Device serial number from PagSeguro SDK, or null if not available
     */
    fun getDeviceSerial(): String? {
        return SdkInstance.getDeviceSerial()
    }

    /**
     * Check if the SDK is initialized
     *
     * @return true if the SDK is initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return SdkInstance.isInitialized()
    }
}