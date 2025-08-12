package br.com.ticpass.pos.queue.processors.printing.processors

import br.com.ticpass.pos.queue.processors.printing.processors.core.DynamicPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import br.com.ticpass.pos.queue.processors.printing.processors.impl.AcquirerPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.impl.MP4200HSPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.impl.MPTIIPrintingProcessor

/**
 * Printing Processor Registry
 * Singleton registry for printing processor instances
 * Ensures processors are only instantiated once and provides a clear place for processor registration
 */
object PrintingProcessorRegistry {
    // Processor instances (created lazily)
    private val acquirerProcessor by lazy { AcquirerPrintingProcessor() }
    private val mp4200HS by lazy { MP4200HSPrintingProcessor() }
    private val mptII by lazy { MPTIIPrintingProcessor() }

    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<PrintingProcessorType, PrintingProcessorBase> by lazy {
        mapOf(
            PrintingProcessorType.ACQUIRER to acquirerProcessor,
            PrintingProcessorType.MP_4200_HS to mp4200HS,
            PrintingProcessorType.MPT_II to mptII,
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicPrintingProcessor {
        return DynamicPrintingProcessor(processorMap)
    }
}
