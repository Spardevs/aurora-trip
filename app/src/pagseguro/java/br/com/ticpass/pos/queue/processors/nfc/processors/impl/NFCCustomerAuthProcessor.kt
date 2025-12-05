package br.com.ticpass.pos.core.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.core.nfc.models.NFCTagCustomerData
import br.com.ticpass.pos.core.nfc.models.NFCTagData
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.input.UserInputRequest
import br.com.ticpass.pos.core.queue.models.NFCError
import br.com.ticpass.pos.core.queue.models.NFCSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCTagMapper
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * PagSeguro NFC Auth Processor
 * Processes NFC authentication using the PagSeguro PlugPag SDK via constructor injection.
 */
class NFCCustomerAuthProcessor @Inject constructor(
    nfcOperations: NFCOperations
) : NFCProcessorBase(nfcOperations) {

    private val TAG = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.CustomerAuthOperation

    override suspend fun process(item: NFCQueueItem.CustomerAuthOperation): ProcessingResult {
        try {
            _item = item

            val result = withContext(Dispatchers.IO) {
                doAuth()
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
     * PagSeguro-specific abort logic
     * Cancels any ongoing nfc transaction
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
     * Requests the user to input NFC tag pin and validates subject ID.
     * @param pin The PIN from the tag
     * @param subjectId The subject ID from the tag to validate against
     */
    private suspend fun requestNFCTagAuth(pin: String, subjectId: String): Boolean {
        return withContext(Dispatchers.IO) {
            requestUserInput(
                UserInputRequest.CONFIRM_NFC_TAG_AUTH(pin = pin, subjectId = subjectId)
            )
        }.value as? Boolean ?: false
    }

    /**
     * Parses NFCTagData into NFCTagCustomerData by extracting JSON from sectors
     * @param tagData The NFCTagData result from readCustomerData
     * @return NFCTagCustomerData object or null if parsing fails
     */
    private fun parseCustomerData(tagData: NFCTagData): NFCTagCustomerData? {
        try {
            // Extract JSON data from sectors starting from sector 1 (skip sector 0 - manufacturer data)
            val jsonBytes = mutableListOf<Byte>()

            for ((sectorIndex, sector) in tagData.sectors.withIndex()) {
                // Skip sector 0 as it contains manufacturer data and header
                if (sectorIndex == 0) continue

                // Extract data from blocks (skip sector trailer - last block)
                val dataBlocks = sector.blocks.dropLast(1)
                for (block in dataBlocks) {
                    jsonBytes.addAll(block.toList())
                }
            }

            // Convert bytes to string and find the end of JSON data
            val jsonString = jsonBytes.toByteArray().toString(Charsets.UTF_8)

            // Find the first null byte or end of meaningful data
            val endIndex = jsonString.indexOf('\u0000')
            val cleanJsonString = if (endIndex != -1) {
                jsonString.substring(0, endIndex)
            } else {
                jsonString
            }.trim()

            Log.d(TAG, "📋 Extracted JSON string: $cleanJsonString")

            if (cleanJsonString.isEmpty()) {
                Log.w(TAG, "❌ No JSON data found in NFC tag")
                return null
            }

            // Parse JSON
            val jsonObject = JSONObject(cleanJsonString)

            return NFCTagCustomerData(
                id = jsonObject.optString("id", ""),
                name = jsonObject.optString("name", ""),
                nationalId = jsonObject.optString("nationalId", ""),
                phone = jsonObject.optString("phone", ""),
                pin = jsonObject.optString("pin", ""),
                subjectId = jsonObject.optString("subjectId", "")
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing customer data: ${e.message}", e)
            return null
        }
    }

    /**
     * Performs the NFC authentication process.
     */
    private suspend fun doAuth(): ProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                detectTag(30_000L)

                _events.tryEmit(NFCEvent.VALIDATING_SECTOR_KEYS)
                val ownedKeys = requestNFCKeys()
                val sectorKeys = NFCTagSectorKeys(
                    typeA = ownedKeys[NFCTagSectorKeyType.A],
                    typeB = ownedKeys[NFCTagSectorKeyType.B],
                )

                _events.tryEmit(NFCEvent.READING_TAG_CUSTOMER_DATA)
                val result = NFCTagMapper.readCustomerData(sectorKeys)
                    ?: throw NFCException(ProcessingErrorEvent.NFC_READING_TAG_CUSTOMER_DATA_ERROR)

                // Parse the result into customer data
                _events.tryEmit(NFCEvent.PROCESSING_TAG_CUSTOMER_DATA)
                val customerData = parseCustomerData(result)
                    ?: throw NFCException(ProcessingErrorEvent.NFC_PROCESSING_TAG_CUSTOMER_DATA_ERROR)

                val didAuth = requestNFCTagAuth(customerData.pin, customerData.subjectId)
                if (!didAuth) throw NFCException(ProcessingErrorEvent.NFC_TAG_CUSTOMER_PIN_INCORRECT)

                return@withContext NFCSuccess.CustomerAuthSuccess(
                    id = customerData.id,
                    name = customerData.name,
                    nationalId = customerData.nationalId,
                    phone = customerData.phone,
                    subjectId = customerData.subjectId
                )
            }
            catch (e: NFCException) {
                throw e
            }
            catch (e: AcquirerNFCException) {
                throw e
            }
            catch (e: PlugPagException) {
                val exception = AcquirerNFCException(e.errorCode, null)
                throw exception
            }
            catch (e: Exception) {
                throw e
            }
        }
    }
}
