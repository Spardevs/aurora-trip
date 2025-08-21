package br.com.ticpass.pos.queue.processors.nfc.processors.impl

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCTagCustomerData
import br.com.ticpass.pos.nfc.models.NFCTagCustomerDataInput
import br.com.ticpass.pos.nfc.models.NFCTagDataHeader
import br.com.ticpass.pos.nfc.models.NFCTagDataType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.NFCError
import br.com.ticpass.pos.queue.models.NFCSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCTagReaderAntenna
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCTagWriter
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCUtils
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
 * Pagseguro NFC Setup Processor
 * Formats card, sets owned keys, and configures secure access bits
 */
class NFCCustomerSetupProcessor : NFCProcessorBase() {

    private val TAG = this.javaClass.simpleName
    private val plugpag = AcquirerSdk.nfc.getInstance()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: NFCQueueItem.CustomerSetupOperation
    private val antenna = NFCTagReaderAntenna

    override suspend fun process(item: NFCQueueItem.CustomerSetupOperation): ProcessingResult {
        try {
            _item = item

            val result = withContext(Dispatchers.IO) {
                doSetup()
            }

            cleanup()

            return result
        }
        catch (exception: NFCException) {
            return NFCError(exception.error)
        }
        catch (exception: AcquirerNFCException) {
            return NFCError(exception.event)
        }
        catch (e: PlugPagException) {
            val exception = AcquirerNFCException(e.errorCode, null)
            return NFCError(exception.event)
        }
        catch (exception: Exception) {
            return NFCError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Pagseguro-specific abort logic
     * Cancels any ongoing nfc transaction
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
     * Writes the customer data header to sector 0, block 1
     */
    private suspend fun writeCustomerDataHeader(header: NFCTagDataHeader, keys: Map<NFCTagSectorKeyType, String>) {
        withContext(Dispatchers.IO) {
            try {
                val sectorKeys = NFCTagSectorKeys(
                    typeA = keys[NFCTagSectorKeyType.A],
                    typeB = keys[NFCTagSectorKeyType.B]
                )

                val headerData = header.toByteArray()
                val success = NFCTagWriter.writeBlock(0, 1, headerData, sectorKeys)

                if (!success) throw NFCException(ProcessingErrorEvent.NFC_WRITE_ERROR)
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
                throw NFCException(ProcessingErrorEvent.NFC_WRITE_ERROR)
            }
        }
    }

    /**
     * Distributes the customer data across NFC sectors
     * @param customerData The JSON data to write
     */
    private suspend fun setCustomerData(customerData: ByteArray, keys: Map<NFCTagSectorKeyType, String>) {
        val blockSize = 16 // MIFARE Classic block size
        val blocksPerSector = 3 // Available data blocks per sector (excluding sector trailer)
        val bytesPerSector = blockSize * blocksPerSector // 48 bytes per sector

        // Skip sector 0 (manufacturer data) and start from sector 1
        var currentSector = 1
        var dataOffset = 0

        val sectorKeys = NFCTagSectorKeys(
            typeA = keys[NFCTagSectorKeyType.A],
            typeB = keys[NFCTagSectorKeyType.B]
        )

        var lastSector = currentSector
        var lastBlock = 0

        while (dataOffset < customerData.size) {
            try {
                Log.d(TAG, "💾 Writing to sector $currentSector")

                // Write data blocks in this sector (blocks 0, 1, 2)
                for (blockNum in 0 until blocksPerSector) {
                    val blockOffset = dataOffset + (blockNum * blockSize)

                    if (blockOffset >= customerData.size) {
                        // No more data to write, fill with zeros
                        val emptyBlock = ByteArray(blockSize)
                        val didWrite = NFCTagWriter.writeBlock(
                            currentSector,
                            blockNum,
                            emptyBlock,
                            sectorKeys
                        )

                        if (!didWrite) {
                            Log.e(TAG, "❌ Failed to write empty block $blockNum in sector $currentSector")
                            throw NFCException(ProcessingErrorEvent.NFC_WRITE_ERROR)
                        }
                    }
                    else {
                        // Prepare block data
                        val blockData = ByteArray(blockSize)
                        val bytesToCopy = minOf(blockSize, customerData.size - blockOffset)

                        customerData.copyInto(
                            blockData,
                            0,
                            blockOffset,
                            blockOffset + bytesToCopy
                        )

                        val didWrite = NFCTagWriter.writeBlock(
                            currentSector,
                            blockNum,
                            blockData,
                            sectorKeys
                        )

                        if (!didWrite) {
                            Log.e(TAG, "❌ Failed to write block $blockNum in sector $currentSector")
                            throw NFCException(ProcessingErrorEvent.NFC_WRITE_ERROR)
                        }

                        Log.d(TAG, "✅ Written block $blockNum in sector $currentSector ($bytesToCopy bytes)")

                        // Track the last sector and block that contains actual data
                        lastSector = currentSector
                        lastBlock = blockNum
                    }
                }

                dataOffset += bytesPerSector
                currentSector++

                Log.i(TAG, "✅ Sector $currentSector data written")
            }
            catch (e: PlugPagException) {
                val exception = AcquirerNFCException(e.errorCode, null)
                throw exception
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Error writing to sector $currentSector: ${e.message}", e)
                throw NFCException(ProcessingErrorEvent.NFC_WRITE_ERROR)
            }
        }

        // Write header with customer data boundaries
        val header = NFCTagDataHeader(
            dataType = NFCTagDataType.CUSTOMER,
            endSector = lastSector,
            endBlock = lastBlock,
            totalBytes = customerData.size
        )
        writeCustomerDataHeader(header, keys)
    }

    /**
     * Validates the provided NFC sector keys.
     */
    private fun validateKeys(keys: Map<NFCTagSectorKeyType, String>) {
        val keysAreValid = NFCUtils.validateKeys(keys)
        if(!keysAreValid) throw NFCException(ProcessingErrorEvent.NFC_TAG_INVALID_KEYS)
    }

    /**
     * Processes customer data and converts it to a JSON byte array.
     * @param customerData The customer data to process
     * @return ByteArray containing the JSON representation of the customer data
     */
    private fun processCustomerData(customerData: NFCTagCustomerData): ByteArray {
        val (id, name, nationalId, phone, pin) = customerData
        val json = """{"name":"$name","nationalId":"$nationalId","phone":"$phone","id":"$id","pin":"$pin"}"""
        val jsonBytes = json.toByteArray(Charsets.UTF_8)

        return jsonBytes
    }

    /**
     * Requests to input customer data for the NFC tag.
     */
    private suspend fun requestNFCTagCustomerData(): NFCTagCustomerData {
        val input = withContext(Dispatchers.IO) {
            requestUserInput(
                UserInputRequest.CONFIRM_NFC_TAG_CUSTOMER_DATA()
            )
        }.value as? NFCTagCustomerDataInput ?: throw NFCException(ProcessingErrorEvent.CANCELLED_BY_USER)

        return NFCTagCustomerData(
            id = input.id,
            name = input.name,
            nationalId = input.nationalId,
            phone = input.phone,
            pin = (1000..9999).random().toString(10)
        )
    }

    /**
     * Request customer to save or memorize their PIN after NFC tag setup.
     * Always returns true, since we don't want abort the operation.
     * The intention is to give the user a few seconds to save or memorize the PIN before proceeding
     * to the next processor.
     */
    private suspend fun requestNFCCustomerSavePIN(pin: String): Boolean {
        try {
            withContext(Dispatchers.IO) {
                requestUserInput(
                    UserInputRequest.CONFIRM_NFC_TAG_CUSTOMER_SAVE_PIN(pin = pin)
                )
            }.value as? Boolean ?: true
        } catch (_: Exception) {}

        return true
    }

    /**
     * Performs the NFC setup operation.
     */
    private suspend fun doSetup(): ProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                _events.tryEmit(NFCEvent.VALIDATING_SECTOR_KEYS)
                val ownedKeys = requestNFCKeys()
                validateKeys(ownedKeys)

                val customerData = requestNFCTagCustomerData()
                _events.tryEmit(NFCEvent.PROCESSING_TAG_CUSTOMER_DATA)
                val customerDataBytes = processCustomerData(customerData)

                detectTag(30_000L)
                _events.tryEmit(NFCEvent.SAVING_TAG_CUSTOMER_DATA)
                setCustomerData(customerDataBytes, ownedKeys)

                val didSavePin = requestNFCCustomerSavePIN(customerData.pin)

                return@withContext NFCSuccess.CustomerSetupSuccess(
                    id = customerData.id,
                    name = customerData.name,
                    nationalId = customerData.nationalId,
                    phone = customerData.phone,
                    pin = customerData.pin
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