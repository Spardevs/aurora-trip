package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.pos.sdk.factory.AcquirerNFCProvider
import br.com.ticpass.pos.sdk.factory.AcquirerPrintingProvider
import br.com.ticpass.pos.sdk.factory.AcquirerRefundProvider
import br.com.ticpass.pos.sdk.factory.CustomerReceiptProvider
import br.com.ticpass.pos.sdk.factory.TransactionProvider
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
 * This class provides direct access to acquirer provider instances for the Stone flavor.
 */
object AcquirerSdk {
    /**
     * Stone payment provider instance
     */
    val payment: BasePaymentProvider<Pair<TransactionProvider, CustomerReceiptProvider>>
        get() = PaymentProvider

    /**
     * Stone printing provider instance
     */
    val printing: BasePrintingProvider<AcquirerPrintingProvider>
        get() = PrintingProvider

    /**
     * Stone NFC provider instance
     */
    val nfc: BaseNFCProvider<AcquirerNFCProvider>
        get() = NFCProvider

    /**
     * Stone refund provider instance
     */
    val refund: BaseRefundProvider<AcquirerRefundProvider>
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
     * Get the Stone Code from the initialized SDK
     *
     * @return The Stone Code
     * @throws IllegalStateException if the SDK is not initialized
     */
    fun getStoneCode(): String {
        return SdkInstance.getStoneCode()
    }

    /**
     * Get the device serial (not applicable for Stone flavor)
     *
     * @return null for Stone flavor (use DeviceUtils.getDeviceSerial instead)
     */
    fun getDeviceSerial(): String? {
        return null
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