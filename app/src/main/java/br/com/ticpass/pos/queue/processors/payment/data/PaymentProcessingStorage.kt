package br.com.ticpass.pos.queue.processors.payment.data

import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.core.QueueStorage
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.queue.processors.payment.utils.toEntity
import br.com.ticpass.pos.queue.processors.payment.utils.toQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Payment Storage
 * Implements QueueStorage interface with Room database
 */
class PaymentProcessingStorage(private val dao: PaymentProcessingQueueDao) : QueueStorage<PaymentProcessingQueueItem> {
    override suspend fun insert(item: PaymentProcessingQueueItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun update(item: PaymentProcessingQueueItem) {
        dao.update(item.toEntity())
    }
    
    override suspend fun getNextPending(): PaymentProcessingQueueItem? {
        return dao.getNextPending()?.toQueueItem()
    }
    
    override suspend fun updateStatus(item: PaymentProcessingQueueItem, status: QueueItemStatus) {
        dao.updateStatus(item.id, status.name)
    }
    
    override suspend fun remove(item: PaymentProcessingQueueItem) {
        dao.delete(item.id)
    }

    override suspend fun removeByStatus(statuses: List<QueueItemStatus>) {
        dao.deleteByStatus(statuses.map { it.name })
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<PaymentProcessingQueueItem> {
        return dao.getAllByStatus(
            statuses.map { it.name }
        ).map { it.toQueueItem() }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<PaymentProcessingQueueItem>> {
        return dao.observeByStatus(status.name).map { entities -> 
            entities.map { it.toQueueItem() }
        }
    }
}
