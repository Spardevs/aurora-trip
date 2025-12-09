package br.com.ticpass.pos.core.queue.processors.nfc.processors.impl

import android.util.Log
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
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCBalanceStorage
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Stone NFC Balance Read Processor
 * Reads balance data from NFC tag using the Stone SDK.
 */
class NFCBalanceReadProcessor @Inject constructor(
    nfcOperations: NFCOperations,
    private val balanceStorage: NFCBalanceStorage
) : NFCProcessorBase(nfcOperations) {

    private val TAG = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.BalanceReadOperation

    override suspend fun process(item: NFCQueueItem.BalanceReadOperation): ProcessingResult {
        try {
            _item = item

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
     * Performs the NFC balance read process.
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

                _events.tryEmit(NFCEvent.READING_TAG_BALANCE_DATA)

                // Read balance from tag
                val balanceHeader = balanceStorage.readBalance(sectorKeys)
                
                if (balanceHeader == null) {
                    Log.d(TAG, "ℹ️ No balance data found on tag, returning 0")
                    return@withContext NFCSuccess.BalanceReadSuccess(
                        balance = 0u,
                        timestamp = System.currentTimeMillis()
                    )
                }

                Log.i(TAG, "✅ Balance read: ${balanceHeader.formattedBalance()}")
                
                return@withContext NFCSuccess.BalanceReadSuccess(
                    balance = balanceHeader.balance,
                    timestamp = balanceHeader.timestamp
                )
            }
            catch (e: NFCException) {
                throw e
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Error reading balance: ${e.message}", e)
                throw NFCException(ProcessingErrorEvent.NFC_BALANCE_READ_ERROR)
            }
        }
    }

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

    private fun cleanupCoroutineScopes() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    private fun cleanup() {
        cleanupCoroutineScopes()
    }
}
