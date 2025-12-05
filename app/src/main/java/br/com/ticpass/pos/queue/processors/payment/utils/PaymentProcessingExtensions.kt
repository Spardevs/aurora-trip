package br.com.ticpass.pos.queue.processors.payment.utils

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEntity
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem

/**
 * Extension functions for converting between PaymentProcessingEntity and PaymentProcessingQueueItem
 */

/**
 * Convert PaymentProcessingQueueItem to PaymentProcessingEntity
 */
fun PaymentProcessingQueueItem.toEntity(): PaymentProcessingEntity {
    return PaymentProcessingEntity(
        id = id,
        priority = priority,
        status = status.toString(),
        amount = amount,
        commission = commission,
        method = method.value,
    )
}

/**
 * Convert PaymentProcessingEntity to PaymentProcessingQueueItem
 */
fun PaymentProcessingEntity.toQueueItem(): PaymentProcessingQueueItem {
    return PaymentProcessingQueueItem(
        id = id,
        priority = priority,
        status = QueueItemStatus.valueOf(status.uppercase()),
        amount = amount,
        commission = commission,
        method = SystemPaymentMethod.fromValue(method),
        isTransactionless = false
    )
}
