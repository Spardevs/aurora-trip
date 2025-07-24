package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.QueueItemStatus
import br.com.ticpass.pos.queue.QueueStorage
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
    
    override suspend fun getNextPending(): ProcessingPaymentQueueItem? {
        return dao.getNextPending()?.toQueueItem()
    }
    
    override suspend fun updateStatus(item: ProcessingPaymentQueueItem, status: QueueItemStatus) {
        dao.updateStatus(item.id, status.name)
    }
    
    override suspend fun remove(item: ProcessingPaymentQueueItem) {
        dao.delete(item.id)
    }
    
    override suspend fun getAllByStatus(status: QueueItemStatus): List<ProcessingPaymentQueueItem> {
        return dao.getAllByStatus(status.name).map { it.toQueueItem() }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<ProcessingPaymentQueueItem>> {
        return dao.observeByStatus(status.name).map { entities -> 
            entities.map { it.toQueueItem() }
        }
    }
}
