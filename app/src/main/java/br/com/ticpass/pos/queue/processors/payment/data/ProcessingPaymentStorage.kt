package br.com.ticpass.pos.queue.processors.payment.data

import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.core.QueueStorage
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.processors.payment.utils.toEntity
import br.com.ticpass.pos.queue.processors.payment.utils.toQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Payment Storage
 * Implements QueueStorage interface with Room database
 */
class ProcessingPaymentStorage(private val dao: ProcessingPaymentQueueDao) : QueueStorage<ProcessingPaymentQueueItem> {
    override suspend fun insert(item: ProcessingPaymentQueueItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun update(item: ProcessingPaymentQueueItem) {
        dao.update(item.toEntity())
    }
    
    override suspend fun getNextPending(): ProcessingPaymentQueueItem? {
        return dao.getNextPending()?.toQueueItem()
    }
    
    override suspend fun updateStatus(item: ProcessingPaymentQueueItem, status: QueueItemStatus) {
        dao.updateStatus(item.id, status.name)
    }
    
    override suspend fun remove(item: ProcessingPaymentQueueItem) {
        dao.delete(item.id)
    }

    override suspend fun removeByStatus(statuses: List<QueueItemStatus>) {
        dao.deleteByStatus(statuses.map { it.name })
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<ProcessingPaymentQueueItem> {
        return dao.getAllByStatus(
            statuses.map { it.name }
        ).map { it.toQueueItem() }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<ProcessingPaymentQueueItem>> {
        return dao.observeByStatus(status.name).map { entities -> 
            entities.map { it.toQueueItem() }
        }
    }
}
