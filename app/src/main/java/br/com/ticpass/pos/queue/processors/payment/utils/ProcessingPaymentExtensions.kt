package br.com.ticpass.pos.queue.processors.payment.utils

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEntity
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem

/**
 * Extension functions for converting between ProcessingPaymentEntity and ProcessingPaymentQueueItem
 */

/**
 * Convert ProcessingPaymentQueueItem to ProcessingPaymentEntity
 */
fun ProcessingPaymentQueueItem.toEntity(): ProcessingPaymentEntity {
    return ProcessingPaymentEntity(
        id = id,
        priority = priority,
        status = status.toString(),
        amount = amount,
        commission = commission,
        method = method.value,
    )
}

/**
 * Convert ProcessingPaymentEntity to ProcessingPaymentQueueItem
 */
fun ProcessingPaymentEntity.toQueueItem(): ProcessingPaymentQueueItem {
    return ProcessingPaymentQueueItem(
        id = id,
        priority = priority,
        status = QueueItemStatus.valueOf(status.uppercase()),
        amount = amount,
        commission = commission,
        method = SystemPaymentMethod.fromValue(method),
        isTransactionless = false
    )
}
