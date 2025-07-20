package br.com.ticpass.pos.queue.printing

import br.com.ticpass.pos.queue.QueueItem
import br.com.ticpass.pos.queue.QueueItemStatus
import java.util.UUID

/**
 * Print Queue Item
 * Represents a print job in the queue
 */
data class PrintQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 1,
    val content: String,
    val copies: Int = 1,
    val paperSize: PaperSize = PaperSize.RECEIPT,
    val printerId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    override val status: QueueItemStatus
) : QueueItem {
    /**
     * Available paper sizes for printing
     */
    enum class PaperSize {
        RECEIPT, // Standard receipt
        A4,      // Standard A4
        LETTER   // US Letter
    }
}
