package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.pos.queue.processors.factory.CustomerReceiptProvider
import br.com.ticpass.pos.queue.processors.factory.TransactionProvider
import br.com.ticpass.pos.sdk.nfc.BaseNFCProvider
import br.com.ticpass.pos.sdk.nfc.NFCProvider
import br.com.ticpass.pos.sdk.payment.BasePaymentProvider
import br.com.ticpass.pos.sdk.payment.PaymentProvider
import br.com.ticpass.pos.sdk.printing.BasePrintingProvider
import br.com.ticpass.pos.sdk.printing.PrintingProvider
import stone.user.UserModel


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
    val printing: BasePrintingProvider<UserModel>
        get() = PrintingProvider

    /**
     * Stone NFC provider instance
     */
    val nfc: BaseNFCProvider<UserModel>
        get() = NFCProvider

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