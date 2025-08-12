package br.com.ticpass.pos.sdk.factory

import android.content.Context
import br.com.stone.posandroid.providers.PosTransactionProvider
import stone.database.transaction.TransactionObject
import stone.user.UserModel

typealias TransactionProvider = (TransactionObject) -> PosTransactionProvider

/**
 * Factory for creating PosTransactionProvider instances
 * 
 * This implements a higher-order function.
 * Initialize the factory with context and usermodel, then use it with only
 * the TransactionObject as needed.
 */
class TransactionProviderFactory(
    private val context: Context,
    private val usermodel: UserModel
) {
    /**
     * Creates a function that takes only a TransactionObject and returns a PosTransactionProvider
     * This follows the higher-order function pattern from functional programming
     * 
     * @return A function that creates a PosTransactionProvider with the configured context and usermodel
     */
    fun create(): TransactionProvider {
        return { transaction: TransactionObject ->
            PosTransactionProvider(
                context,
                transaction,
                usermodel
            )
        }
    }
}
