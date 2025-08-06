package br.com.ticpass.pos.sdk.payment

import android.content.Context
import br.com.ticpass.pos.queue.processors.factory.CustomerReceiptProviderFactory
import br.com.ticpass.pos.queue.processors.factory.CustomerReceiptProvider
import br.com.ticpass.pos.queue.processors.factory.TransactionProvider
import br.com.ticpass.pos.queue.processors.factory.TransactionProviderFactory
import br.com.ticpass.pos.sdk.SdkInstance

/**
 * Stone-specific implementation of PaymentProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object PaymentProvider : BasePaymentProvider<Pair<TransactionProvider, CustomerReceiptProvider>> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            // Use the shared SDK instance
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): Pair<TransactionProvider, CustomerReceiptProvider> {
        if (!isInitialized()) {
            throw IllegalStateException("Payment provider not initialized. Call initialize() first.")
        }
        val (userModel, context) =  SdkInstance.getInstance()
        val transactionFactory = TransactionProviderFactory(context, userModel)
        val customerReceiptFactory = CustomerReceiptProviderFactory(context)

        return Pair(
            transactionFactory.create(),
            customerReceiptFactory.create()
        )
    }
}