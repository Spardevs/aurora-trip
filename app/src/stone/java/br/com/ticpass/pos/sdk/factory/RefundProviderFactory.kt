package br.com.ticpass.pos.sdk.factory

import android.content.Context
import stone.database.transaction.TransactionDAO
import stone.database.transaction.TransactionObject
import stone.providers.CancellationProvider

typealias AcquirerRefundProvider = (atk: String) -> Pair<CancellationProvider, TransactionObject>

/**
 * Factory for creating AcquirerRefundProvider instances
 * 
 * This implements a higher-order function.
 * Initialize the factory with context, then use it with only
 * the CancellationProvider as needed.
 */
class RefundProviderFactory(
    private val context: Context
) {
    /**
     * Creates a function that takes only ATK and returns a AcquirerRefundProvider
     * This follows the higher-order function pattern from functional programming
     * 
     * @return A function that creates a AcquirerRefundProvider with the configured context.
     */
    fun create(): AcquirerRefundProvider {
        return { atk: String ->
            val dao = TransactionDAO(context)
            val transaction = dao.findTransactionWithAtk(atk) ?: TransactionObject()

            Pair(
                CancellationProvider(context, transaction),
                transaction
            )
        }
    }
}
