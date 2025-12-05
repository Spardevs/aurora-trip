package br.com.ticpass.pos.queue.processors.refund.processors

import br.com.ticpass.pos.queue.processors.refund.processors.core.DynamicRefundProcessor
import br.com.ticpass.pos.queue.processors.refund.processors.core.RefundProcessorBase
import br.com.ticpass.pos.queue.processors.refund.processors.impl.AcquirerRefundProcessor
import br.com.ticpass.pos.queue.processors.refund.processors.models.RefundProcessorType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refund Processor Registry
 * Injectable registry for refund processor instances.
 * Uses Hilt to inject processor dependencies.
 */
@Singleton
class RefundProcessorRegistry @Inject constructor(
    private val acquirerProcessor: AcquirerRefundProcessor
) {
    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<RefundProcessorType, RefundProcessorBase> by lazy {
        mapOf(
//            RefundProcessorType.ACQUIRER to acquirerProcessor,
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicRefundProcessor {
        return DynamicRefundProcessor(processorMap)
    }
}
