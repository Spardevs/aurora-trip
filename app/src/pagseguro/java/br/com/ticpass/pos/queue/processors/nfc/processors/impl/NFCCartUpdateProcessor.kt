package br.com.ticpass.pos.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.nfc.models.CartOperation
import br.com.ticpass.pos.nfc.models.NFCCartItem
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.NFCError
import br.com.ticpass.pos.queue.models.NFCSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCCartStorage
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCTagReaderAntenna
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PagSeguro NFC Cart Update Processor
 * Updates cart items on NFC tag (add, remove, increment, decrement)
 */
class NFCCartUpdateProcessor : NFCProcessorBase() {

    private val TAG = this.javaClass.simpleName
    private val plugpag = AcquirerSdk.nfc.getInstance()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.CartUpdateOperation
    private val antenna = NFCTagReaderAntenna

    override suspend fun process(item: NFCQueueItem.CartUpdateOperation): ProcessingResult {
        try {
            _item = item

            val result = withContext(Dispatchers.IO) {
                doUpdate()
            }

            cleanup()

            return result
        }
        catch (e: NFCException) {
            return NFCError(e.error)
        }
        catch (e: AcquirerNFCException) {
            return NFCError(e.event)
        }
        catch (e: PlugPagException) {
            val exception = AcquirerNFCException(e.errorCode, null)
            return NFCError(exception.event)
        }
        catch (e: Exception) {
            return NFCError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Performs the NFC cart update process.
     */
    private suspend fun doUpdate(): ProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                detectTag(_item.timeout)

                _events.tryEmit(NFCEvent.VALIDATING_SECTOR_KEYS)
                val ownedKeys = requestNFCKeys()
                val sectorKeys = NFCTagSectorKeys(
                    typeA = ownedKeys[NFCTagSectorKeyType.A],
                    typeB = ownedKeys[NFCTagSectorKeyType.B],
                )

                // Validate that customer data exists first
                val customerHeader = NFCCartStorage.readCustomerHeader(sectorKeys)
                if (customerHeader == null) {
                    Log.e(TAG, "❌ Customer data not found - card must be set up first")
                    throw NFCException(ProcessingErrorEvent.NFC_READING_TAG_CUSTOMER_DATA_ERROR)
                }

                _events.tryEmit(NFCEvent.READING_TAG_CART_DATA)

                // Read existing cart
                val cartHeader = NFCCartStorage.readCartHeader(sectorKeys)
                val existingItems = if (cartHeader != null && cartHeader.itemCount > 0) {
                    NFCCartStorage.readCart(cartHeader, sectorKeys)
                } else {
                    emptyList()
                }

                Log.d(TAG, "📋 Current cart: ${existingItems.size} items")

                // Modify cart based on operation
                _events.tryEmit(NFCEvent.PROCESSING_TAG_CART_DATA)
                val updatedItems = modifyCart(existingItems, _item.productId, _item.quantity, _item.operation)

                Log.d(TAG, "📝 Updated cart: ${updatedItems.size} items")

                // Calculate where cart data starts
                val cartStartSector = if (customerHeader.endBlock == 2) {
                    customerHeader.endSector + 1
                } else {
                    customerHeader.endSector
                }
                val cartStartBlock = if (customerHeader.endBlock == 2) {
                    0
                } else {
                    customerHeader.endBlock + 1
                }

                // Check if we have enough space
                val bytesNeeded = updatedItems.size * NFCCartItem.SIZE_BYTES
                val availableSpace = NFCCartStorage.calculateAvailableSpace(customerHeader)

                if (bytesNeeded > availableSpace) {
                    Log.e(TAG, "❌ Insufficient space: need $bytesNeeded bytes, have $availableSpace bytes")
                    throw NFCException(ProcessingErrorEvent.NFC_INSUFFICIENT_SPACE)
                }

                Log.d(TAG, "📊 Space check: $bytesNeeded/$availableSpace bytes")

                // Write updated cart
                _events.tryEmit(NFCEvent.WRITING_TAG_CART_DATA)
                val newCartHeader = NFCCartStorage.writeCart(
                    updatedItems,
                    cartStartSector,
                    cartStartBlock,
                    sectorKeys
                )

                // Update cart header
                NFCCartStorage.writeCartHeader(newCartHeader, sectorKeys)

                Log.i(TAG, "✅ Cart updated successfully: ${updatedItems.size} items")
                
                return@withContext NFCSuccess.CartUpdateSuccess(updatedItems)
            }
            catch (e: NFCException) {
                throw e
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Error updating cart: ${e.message}", e)
                throw NFCException(ProcessingErrorEvent.NFC_CART_WRITE_ERROR)
            }
        }
    }

    /**
     * Modifies the cart based on the operation
     */
    private fun modifyCart(
        existingItems: List<NFCCartItem>,
        productId: UShort,
        quantity: UByte,
        operation: CartOperation
    ): List<NFCCartItem> {
        val mutableItems = existingItems.toMutableList()
        val existingIndex = mutableItems.indexOfFirst { it.id == productId }

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
                        // Update existing item
                        mutableItems[existingIndex] = mutableItems[existingIndex].copy(count = quantity)
                        Log.d(TAG, "🔄 Set product $productId quantity to $quantity")
                    } else {
                        // Add new item
                        mutableItems.add(NFCCartItem(productId, quantity))
                        Log.d(TAG, "➕ Added product $productId with quantity $quantity")
                    }
                }
            }

            CartOperation.INCREMENT -> {
                if (existingIndex >= 0) {
                    // Item exists - check for overflow before incrementing
                    val existing = mutableItems[existingIndex]
                    val sum = existing.count.toInt() + quantity.toInt()
                    
                    if (sum > 255) {
                        Log.e(TAG, "❌ Overflow: product $productId has ${existing.count}, adding $quantity would exceed max (255)")
                        throw NFCException(ProcessingErrorEvent.PRODUCT_QUANTITY_OVERFLOW)
                    }
                    
                    val newQuantity = sum.toUByte()
                    mutableItems[existingIndex] = existing.copy(count = newQuantity)
                    Log.d(TAG, "🔄 Incremented product $productId by $quantity: ${existing.count} → $newQuantity")
                } else {
                    // Item doesn't exist - add with specified quantity
                    mutableItems.add(NFCCartItem(productId, quantity))
                    Log.d(TAG, "➕ Added product $productId with quantity $quantity")
                }
            }

            CartOperation.DECREMENT -> {
                if (existingIndex >= 0) {
                    val existing = mutableItems[existingIndex]
                    val newQuantity = (existing.count.toInt() - quantity.toInt()).coerceAtLeast(0)
                    if (newQuantity == 0) {
                        mutableItems.removeAt(existingIndex)
                        Log.d(TAG, "➖ Removed product $productId (quantity reached 0 after decrement by $quantity)")
                    } else {
                        mutableItems[existingIndex] = existing.copy(count = newQuantity.toUByte())
                        Log.d(TAG, "🔄 Decremented product $productId by $quantity: ${existing.count} → $newQuantity")
                    }
                } else {
                    Log.e(TAG, "❌ Product $productId not found in cart for decrement")
                    throw NFCException(ProcessingErrorEvent.NFC_CART_ITEM_NOT_FOUND)
                }
            }

            CartOperation.REMOVE -> {
                if (existingIndex >= 0) {
                    mutableItems.removeAt(existingIndex)
                    Log.d(TAG, "➖ Removed product $productId")
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

    /**
     * PagSeguro-specific abort logic
     * Cancels any ongoing NFC transaction
     */
    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                antenna.stop()
                plugpag.abortNFC()
                cleanup()
            }
            deferred.complete(true)
        }
        catch (exception: Exception) { deferred.complete(false) }

        return deferred.await()
    }

    /**
     * Cancels all coroutines in the current scope and creates a new scope.
     * This ensures that any ongoing operations are properly terminated and
     * resources are released, while maintaining the processor ready for
     * future NFC operations.
     */
    private fun cleanupCoroutineScopes() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Cleans up resources used by the processor.
     * This includes cancelling coroutines and recycling the bitmap if it was initialized.
     */
    private fun cleanup() {
        cleanupCoroutineScopes()
    }
}
