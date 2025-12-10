package br.com.ticpass.pos.core.sdk.factory

import android.content.Context
import br.com.stone.posandroid.providers.PosPrintReceiptProvider
import stone.application.enums.ReceiptType
import stone.database.transaction.TransactionObject

typealias CustomerReceiptProvider = (TransactionObject) -> PosPrintReceiptProvider

/**
 * Factory for creating PosPrintReceiptProvider instances
 * 
 * This implements a higher-order function.
 * Initialize the factory with context, then use it with only
 * the TransactionObject as needed.
 */
class CustomerReceiptProviderFactory(
    private val context: Context
) {
    /**
     * Creates a function that takes only a TransactionObject and returns a PosPrintReceiptProvider
     * This follows the higher-order function pattern from functional programming
     * 
     * @return A function that creates a PosPrintReceiptProvider with the configured context
     */
    fun create(): CustomerReceiptProvider {
        return { transaction: TransactionObject ->
            PosPrintReceiptProvider(
                context,
                transaction,
                ReceiptType.CLIENT
            )
        }
    }
}
