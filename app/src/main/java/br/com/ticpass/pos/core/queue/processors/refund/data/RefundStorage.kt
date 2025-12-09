package br.com.ticpass.pos.core.queue.processors.refund.data

import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.core.QueueStorage
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.utils.toEntity
import br.com.ticpass.pos.core.queue.processors.refund.utils.toQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Refund Storage
 * Implements QueueStorage interface with Room database
 */
class RefundStorage(private val dao: RefundQueueDao) : QueueStorage<RefundQueueItem> {
    override suspend fun insert(item: RefundQueueItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun update(item: RefundQueueItem) {
        dao.update(item.toEntity())
    }
    
    override suspend fun getNextPending(): RefundQueueItem? {
        return dao.getNextPending()?.toQueueItem()
    }
    
    override suspend fun updateStatus(item: RefundQueueItem, status: QueueItemStatus) {
        dao.updateStatus(item.id, status.name)
    }
    
    override suspend fun remove(item: RefundQueueItem) {
        dao.delete(item.id)
    }

    override suspend fun removeByStatus(statuses: List<QueueItemStatus>) {
        dao.deleteByStatus(statuses.map { it.name })
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<RefundQueueItem> {
        return dao.getAllByStatus(
            statuses.map { it.name }
        ).map { it.toQueueItem() }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<RefundQueueItem>> {
        return dao.observeByStatus(status.name).map { entities -> 
            entities.map { it.toQueueItem() }
        }
    }
}
