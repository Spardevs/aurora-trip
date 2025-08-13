package br.com.ticpass.pos.queue.processors.refund.processors

import br.com.ticpass.pos.queue.processors.refund.processors.core.DynamicRefundProcessor
import br.com.ticpass.pos.queue.processors.refund.processors.core.RefundProcessorBase
import br.com.ticpass.pos.queue.processors.refund.processors.models.RefundProcessorType
import br.com.ticpass.pos.queue.processors.refund.processors.impl.AcquirerRefundProcessor

/**
 * Refund Processor Registry
 * Singleton registry for refund processor instances
 * Ensures processors are only instantiated once and provides a clear place for processor registration
 */
object RefundProcessorRegistry {
    // Processor instances (created lazily)
    private val acquirerProcessor by lazy { AcquirerRefundProcessor() }

    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<RefundProcessorType, RefundProcessorBase> by lazy {
        mapOf(
            RefundProcessorType.ACQUIRER to acquirerProcessor,
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicRefundProcessor {
        return DynamicRefundProcessor(processorMap)
    }
}
