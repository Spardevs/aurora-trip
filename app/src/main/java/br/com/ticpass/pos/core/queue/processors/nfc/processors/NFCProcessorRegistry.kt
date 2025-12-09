package br.com.ticpass.pos.core.queue.processors.nfc.processors

import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.DynamicNFCProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCBalanceReadProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCBalanceUpdateProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCCartReadProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCCartUpdateProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCCustomerAuthProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCCustomerSetupProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.impl.NFCTagFormatProcessor
import br.com.ticpass.pos.core.queue.processors.nfc.processors.models.NFCProcessorType
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NFC Processor Registry
 * Injectable registry for NFC processor instances.
 * Uses Hilt to inject processor dependencies.
 */
@Singleton
class NFCProcessorRegistry @Inject constructor(
    private val nfcOperations: NFCOperations,
    private val nfcCustomerAuth: NFCCustomerAuthProcessor,
    private val nfcTagFormat: NFCTagFormatProcessor,
    private val nfcCustomerSetup: NFCCustomerSetupProcessor,
    private val nfcCartRead: NFCCartReadProcessor,
    private val nfcCartUpdate: NFCCartUpdateProcessor,
    private val nfcBalanceRead: NFCBalanceReadProcessor,
    private val nfcBalanceUpdate: NFCBalanceUpdateProcessor
) {
    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<NFCProcessorType, NFCProcessorBase> by lazy {
        mapOf(
            NFCProcessorType.CUSTOMER_AUTH to nfcCustomerAuth,
            NFCProcessorType.TAG_FORMAT to nfcTagFormat,
            NFCProcessorType.CUSTOMER_SETUP to nfcCustomerSetup,
            NFCProcessorType.CART_READ to nfcCartRead,
            NFCProcessorType.CART_UPDATE to nfcCartUpdate,
            NFCProcessorType.BALANCE_READ to nfcBalanceRead,
            NFCProcessorType.BALANCE_UPDATE to nfcBalanceUpdate
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicNFCProcessor {
        return DynamicNFCProcessor(nfcOperations, processorMap)
    }
}
