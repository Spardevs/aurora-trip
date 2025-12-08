package br.com.ticpass.pos.core.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.stone.posandroid.hal.api.mifare.MifareKeyType
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.core.nfc.models.NFCBruteForceProgressCallback
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCBruteForcer
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCTagFormatter
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.NFCError
import br.com.ticpass.pos.core.queue.models.NFCSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.NFCProcessorBase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Stone NFC Format Processor
 * Formats NFC tags using the Stone SDK via constructor injection.
 */
class NFCTagFormatProcessor @Inject constructor(
    nfcOperations: NFCOperations
) : NFCProcessorBase(nfcOperations) {

    private val TAG = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.TagFormatOperation
    private lateinit var ownedKeys: Map<NFCTagSectorKeyType, String>
    private lateinit var foundKeys:  Map<Int, NFCTagSectorKeys>
    private val bruteForceUtil = NFCBruteForcer

    override suspend fun process(item: NFCQueueItem.TagFormatOperation): ProcessingResult {
        try {
            _item = item

            withContext(Dispatchers.IO) {
                doFormat()
            }

            cleanup()

            return NFCSuccess.FormatSuccess()
        }
        catch (e: NFCException) {
            return NFCError(e.error)
        }
        catch (e: AcquirerNFCException) {
            return NFCError(e.event)
        }
        catch (e: PosMifareProvider.MifareException) {
            val acquirerException = AcquirerNFCException(e.errorEnum)
            val exception = NFCError(acquirerException.event)
            return exception
        }
        catch (exception: Exception) {
            return NFCError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Stone-specific abort logic
     * Cancels any ongoing nfc transaction
     */
    override suspend fun onAbort(item: NFCQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                nfcOperations.stopAntenna()
                nfcOperations.abortDetection();
                bruteForceUtil.abortBruteForce()
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
     * future nfc operations.
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

    /**
     * Callback for brute force progress updates.
     * Logs key attempts, found keys, and phase completion.
     */
    private val bruteForceCallback: NFCBruteForceProgressCallback
        get() = object : NFCBruteForceProgressCallback {
            override fun onKeyAttempt(
                currentAttempt: Int,
                maxPossibleAttempts: Int,
                currentKey: String,
                sector: Int,
                keyType: NFCTagSectorKeyType,
                phase: Int
            ) {
                Log.d(TAG, "🔑 Testing key $currentAttempt/$maxPossibleAttempts: " +
                        "$currentKey on sector $sector ($keyType) - Phase $phase")
            }

            override fun onKeyFound(
                sector: Int,
                keyType: NFCTagSectorKeyType,
                key: String,
                completeSectors: Int,
                totalSectors: Int
            ) {
                Log.i(TAG, "✅ Found $keyType key for sector $sector:" +
                        " $key ($completeSectors/$totalSectors complete)")
            }

            override fun onPhaseComplete(
                phase: Int,
                completeSectors: Int,
                partialSectors: Int,
                attempts: Int
            ) {
                Log.i(TAG, "📊 Phase $phase complete: $completeSectors complete, " +
                        "$partialSectors partial sectors, $attempts attempts")
            }
        }

    /**
     * Performs brute force key discovery for NFC sectors.
     */
    private suspend fun doBruteForce(): Map<Int, NFCTagSectorKeys> {
        return withContext(Dispatchers.IO) {
            val result = bruteForceUtil.bruteForceKeys(
                mode = _item.bruteForce,
                progressCallback = bruteForceCallback,
                ownedKeys = ownedKeys.values.toList(),
            )

            val noAuth = result.foundKeys.values.all { !it.isComplete() }
            if(noAuth) throw NFCException(ProcessingErrorEvent.NFC_NOT_AUTHENTICATED)

            return@withContext result.foundKeys
        }
    }

    /**
     * Performs the NFC format operation.
     * Sets up the connection callback, emits processing events, and executes the NFC provider.
     * Awaits the result of the operation and returns it.
     */
    private suspend fun doFormat() {
        return withContext(Dispatchers.IO) {
            try {
                val cardFormatter = NFCTagFormatter()
                detectTag(30_000L)

                ownedKeys = requestNFCKeys()
                _events.tryEmit(NFCEvent.AUTHENTICATING_SECTORS)
                foundKeys = doBruteForce()

                _events.tryEmit(NFCEvent.FORMATTING_TAG)
                cardFormatter.performCardFormat(foundKeys, ownedKeys)
            }
            catch (e: NFCException) {
                throw e
            }
            catch (e: AcquirerNFCException) {
                throw e
            }
            catch (e: PosMifareProvider.MifareException) {
                val exception = AcquirerNFCException(e.errorEnum)
                throw exception
            }
            catch (e: Exception) {
                throw e
            }
        }
    }
}