package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.pos.sdk.nfc.BaseNFCProvider
import br.com.ticpass.pos.sdk.nfc.NFCProvider
import br.com.ticpass.pos.sdk.payment.BasePaymentProvider
import br.com.ticpass.pos.sdk.payment.PaymentProvider
import br.com.ticpass.pos.sdk.printing.BasePrintingProvider
import br.com.ticpass.pos.sdk.printing.PrintingProvider

/**
 * Central access point for acquirer SDK instances
 * 
 * This class provides direct access to acquirer provider instances for the current flavor.
 * The actual implementation classes are provided by the appropriate source set at build time.
 */
object AcquirerSdk {
    // Placeholder for references to provider instances
    // These will be implemented differently in each flavor's source set
    
    /**
     * Reference to payment provider instance
     * The actual implementation comes from the flavor's source set
     */
    val payment: BasePaymentProvider
        get() = PaymentProvider.instance
    
    /**
     * Reference to printing provider instance
     * The actual implementation comes from the flavor's source set
     */
    val printing: BasePrintingProvider
        get() = PrintingProvider.instance
    
    /**
     * Reference to NFC provider instance
     * The actual implementation comes from the flavor's source set
     */
    val nfc: BaseNFCProvider
        get() = NFCProvider.instance
    
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
    }
}