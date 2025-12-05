package br.com.ticpass.pos.core.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.core.nfc.models.CartOperation
import br.com.ticpass.pos.core.nfc.models.NFCCartItem
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.NFCError
import br.com.ticpass.pos.core.queue.models.NFCSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCCartOperations
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCCartStorage
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PagSeguro NFC Cart Update Processor
 * Updates cart items on NFC tag using the PagSeguro PlugPag SDK via constructor injection.
 */
class NFCCartUpdateProcessor @Inject constructor(
    nfcOperations: NFCOperations,
    private val cartStorage: NFCCartStorage
) : NFCProcessorBase(nfcOperations) {

    private val TAG = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.CartUpdateOperation

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
                val customerHeader = cartStorage.readCustomerHeader(sectorKeys)
                if (customerHeader == null) {
                    Log.e(TAG, "❌ Customer data not found - card must be set up first")
                    throw NFCException(ProcessingErrorEvent.NFC_READING_TAG_CUSTOMER_DATA_ERROR)
                }

                _events.tryEmit(NFCEvent.READING_TAG_CART_DATA)

                // Read existing cart
                val cartHeader = cartStorage.readCartHeader(sectorKeys)
                val existingItems = if (cartHeader != null && cartHeader.itemCount > 0) {
                    cartStorage.readCart(cartHeader, sectorKeys)
                } else {
                    emptyList()
                }

                Log.d(TAG, "📋 Current cart: ${existingItems.size} items")

                // Modify cart based on operation
                _events.tryEmit(NFCEvent.PROCESSING_TAG_CART_DATA)
                val updatedItems = NFCCartOperations.modifyCart(existingItems, _item.productId, _item.quantity, _item.price, _item.operation)

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
                val availableSpace = cartStorage.calculateAvailableSpace(customerHeader)

                if (bytesNeeded > availableSpace) {
                    Log.e(TAG, "❌ Insufficient space: need $bytesNeeded bytes, have $availableSpace bytes")
                    throw NFCException(ProcessingErrorEvent.NFC_INSUFFICIENT_SPACE)
                }

                Log.d(TAG, "📊 Space check: $bytesNeeded/$availableSpace bytes")

                // Write updated cart
                _events.tryEmit(NFCEvent.WRITING_TAG_CART_DATA)
                val newCartHeader = cartStorage.writeCart(
                    updatedItems,
                    cartStartSector,
                    cartStartBlock,
                    sectorKeys
                )

                // Update cart header
                cartStorage.writeCartHeader(newCartHeader, sectorKeys)

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
     * PagSeguro-specific abort logic
     * Cancels any ongoing NFC transaction
     */
    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                nfcOperations.stopAntenna()
                nfcOperations.abortDetection()
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
