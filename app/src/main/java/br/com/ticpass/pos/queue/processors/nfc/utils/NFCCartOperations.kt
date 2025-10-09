package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.nfc.models.CartOperation
import br.com.ticpass.pos.nfc.models.NFCCartItem
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException

/**
 * Shared cart operations utility for NFC cart management
 * Contains price-aware logic that allows duplicate product IDs with different prices
 */
object NFCCartOperations {
    private const val TAG = "NFCCartOperations"

    /**
     * Modifies the cart based on the operation with price-aware logic
     * Allows duplicate product IDs with different prices
     * 
     * @param existingItems Current cart items
     * @param productId Product identifier
     * @param quantity Item quantity
     * @param price Price per unit in cents
     * @param operation Cart operation to perform
     * @return Modified list of cart items
     */
    fun modifyCart(
        existingItems: List<NFCCartItem>,
        productId: UShort,
        quantity: UByte,
        price: UInt,
        operation: CartOperation
    ): List<NFCCartItem> {
        val mutableItems = existingItems.toMutableList()
        // Find entry with matching ID AND price for price-aware operations
        val existingIndex = mutableItems.indexOfFirst { it.id == productId && it.price == price }

        when (operation) {
            CartOperation.SET -> {
                if (quantity.toInt() == 0) {
                    // Remove item if quantity is 0
                    if (existingIndex >= 0) {
                        mutableItems.removeAt(existingIndex)
                        Log.d(TAG, "➖ Removed product $productId (quantity set to 0)")
                    }
                } else {
                    if (existingIndex >= 0) {
                        // Update existing item with same price
                        mutableItems[existingIndex] = mutableItems[existingIndex].copy(count = quantity)
                        Log.d(TAG, "🔄 Set product $productId quantity to $quantity at price $${price.toDouble()/100.0}")
                    } else {
                        // Add new item with price
                        mutableItems.add(NFCCartItem(productId, quantity, price))
                        Log.d(TAG, "➕ Added product $productId with quantity $quantity at price $${price.toDouble()/100.0}")
                    }
                }
            }

            CartOperation.INCREMENT -> {
                if (existingIndex >= 0) {
                    // Item exists with same price - check for overflow before incrementing
                    val existing = mutableItems[existingIndex]
                    val sum = existing.count.toInt() + quantity.toInt()
                    
                    if (sum > 255) {
                        Log.e(TAG, "❌ Overflow: product $productId has ${existing.count}, adding $quantity would exceed max (255)")
                        throw NFCException(ProcessingErrorEvent.PRODUCT_QUANTITY_OVERFLOW)
                    }
                    
                    val newQuantity = sum.toUByte()
                    mutableItems[existingIndex] = existing.copy(count = newQuantity)
                    Log.d(TAG, "🔄 Incremented product $productId by $quantity: ${existing.count} → $newQuantity at price $${price.toDouble()/100.0}")
                } else {
                    // Item doesn't exist or different price - create new entry
                    mutableItems.add(NFCCartItem(productId, quantity, price))
                    Log.d(TAG, "➕ Added product $productId with quantity $quantity at price $${price.toDouble()/100.0}")
                }
            }

            CartOperation.DECREMENT -> {
                // LIFO: Decrement from newest (most expensive) entries first
                val matchingItems = mutableItems.filter { it.id == productId }
                if (matchingItems.isEmpty()) {
                    Log.e(TAG, "❌ Product $productId not found in cart for decrement")
                    throw NFCException(ProcessingErrorEvent.NFC_CART_ITEM_NOT_FOUND)
                }
                
                var remainingToDecrement = quantity.toInt()
                // Process in reverse order (LIFO - newest first)
                for (i in mutableItems.indices.reversed()) {
                    if (remainingToDecrement <= 0) break
                    
                    val item = mutableItems[i]
                    if (item.id == productId) {
                        val decrementAmount = minOf(remainingToDecrement, item.count.toInt())
                        val newCount = item.count.toInt() - decrementAmount
                        
                        if (newCount == 0) {
                            mutableItems.removeAt(i)
                            Log.d(TAG, "➖ Removed product $productId entry at price $${item.price.toDouble()/100.0}")
                        } else {
                            mutableItems[i] = item.copy(count = newCount.toUByte())
                            Log.d(TAG, "🔄 Decremented product $productId by $decrementAmount at price $${item.price.toDouble()/100.0}")
                        }
                        
                        remainingToDecrement -= decrementAmount
                    }
                }
            }

            CartOperation.REMOVE -> {
                // Remove ALL entries for this product ID (all prices)
                val removed = mutableItems.removeAll { it.id == productId }
                if (removed) {
                    Log.d(TAG, "➖ Removed all entries for product $productId")
                } else {
                    Log.e(TAG, "❌ Product $productId not found in cart for removal")
                    throw NFCException(ProcessingErrorEvent.NFC_CART_ITEM_NOT_FOUND)
                }
            }

            CartOperation.CLEAR -> {
                mutableItems.clear()
                Log.d(TAG, "🧹 Cleared entire cart (removed ${existingItems.size} items)")
            }
        }

        return mutableItems
    }
}
