package br.com.ticpass.pos.queue.processors.nfc.processors

import br.com.ticpass.pos.queue.processors.nfc.processors.core.DynamicNFCProcessor
import br.com.ticpass.pos.queue.processors.nfc.processors.models.NFCProcessorType
import br.com.ticpass.pos.queue.processors.nfc.processors.core.NFCProcessorBase
import br.com.ticpass.pos.queue.processors.nfc.processors.impl.NFCCustomerAuthProcessor
import br.com.ticpass.pos.queue.processors.nfc.processors.impl.NFCTagFormatProcessor
import br.com.ticpass.pos.queue.processors.nfc.processors.impl.NFCCustomerSetupProcessor

/**
 * NFC Processor Registry
 * Singleton registry for nfc processor instances
 * Ensures processors are only instantiated once and provides a clear place for processor registration
 */
object NFCProcessorRegistry {
    // Processor instances (created lazily)
    private val nfcCustomerAuth by lazy { NFCCustomerAuthProcessor() }
    private val nfcTagFormat by lazy { NFCTagFormatProcessor() }
    private val nfcCustomerSetup by lazy { NFCCustomerSetupProcessor() }

    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<NFCProcessorType, NFCProcessorBase> by lazy {
        mapOf(
//            NFCProcessorType.CUSTOMER_AUTH to nfcCustomerAuth,
//            NFCProcessorType.TAG_FORMAT to nfcTagFormat,
//            NFCProcessorType.CUSTOMER_SETUP to nfcCustomerSetup,
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicNFCProcessor {
        return DynamicNFCProcessor(processorMap)
    }
}
