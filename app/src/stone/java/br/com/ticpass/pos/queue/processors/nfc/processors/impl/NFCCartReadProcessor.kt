package br.com.ticpass.pos.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.stone.posandroid.providers.PosMifareProvider
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
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stone NFC Cart Read Processor
 * Reads cart data from NFC tag
 */
class NFCCartReadProcessor : NFCProcessorBase() {

    private val TAG = this.javaClass.simpleName
    private val nfcProviderFactory = AcquirerSdk.nfc.getInstance()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.CartReadOperation
    private lateinit var nfcProvider: PosMifareProvider

    override suspend fun process(item: NFCQueueItem.CartReadOperation): ProcessingResult {
        try {
            _item = item
            nfcProvider = nfcProviderFactory()

            val result = withContext(Dispatchers.IO) {
                doRead()
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
        catch (e: Exception) {
            return NFCError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Performs the NFC cart read process.
     */
    private suspend fun doRead(): ProcessingResult {
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
                
                // Read cart header
                val cartHeader = NFCCartStorage.readCartHeader(sectorKeys)
                if (cartHeader == null) {
                    Log.w(TAG, "⚠️ No cart header found - returning empty cart")
                    return@withContext NFCSuccess.CartReadSuccess(emptyList())
                }

                Log.d(TAG, "📋 Found cart header: ${cartHeader.itemCount} items, ${cartHeader.totalBytes} bytes")

                // Read cart items
                val items = NFCCartStorage.readCart(cartHeader, sectorKeys)
                
                Log.i(TAG, "✅ Successfully read ${items.size} cart items")
                return@withContext NFCSuccess.CartReadSuccess(items)
            }
            catch (e: NFCException) {
                Log.e(TAG, "❌ NFC Exception: ${e.message}", e)
                throw e
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Error reading cart: ${e.message}", e)
                throw NFCException(ProcessingErrorEvent.NFC_CART_READ_ERROR)
            }
        }
    }

    /**
     * Stone-specific abort logic
     * Cancels any ongoing NFC transaction
     */
    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                nfcProvider.powerOff()
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
