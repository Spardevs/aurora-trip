package br.com.ticpass.pos.queue.processors.printing.processors

import br.com.ticpass.pos.queue.processors.printing.processors.core.DynamicPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.queue.processors.printing.processors.impl.AcquirerPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.impl.MP4200HSPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.impl.MPTIIPrintingProcessor
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Printing Processor Registry
 * Injectable registry for printing processor instances.
 * Uses Hilt to inject processor dependencies.
 */
@Singleton
class PrintingProcessorRegistry @Inject constructor(
    private val acquirerProcessor: AcquirerPrintingProcessor,
    private val mp4200HS: MP4200HSPrintingProcessor,
    private val mptII: MPTIIPrintingProcessor
) {
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
