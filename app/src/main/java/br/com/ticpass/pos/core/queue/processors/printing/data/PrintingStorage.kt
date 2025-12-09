package br.com.ticpass.pos.core.queue.processors.printing.data

import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.core.QueueStorage
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.core.queue.processors.printing.utils.toEntity
import br.com.ticpass.pos.core.queue.processors.printing.utils.toQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Printing Storage
 * Implements QueueStorage interface with Room database
 */
class PrintingStorage(private val dao: PrintingQueueDao) : QueueStorage<PrintingQueueItem> {
    override suspend fun insert(item: PrintingQueueItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun update(item: PrintingQueueItem) {
        dao.update(item.toEntity())
    }
    
    override suspend fun getNextPending(): PrintingQueueItem? {
        return dao.getNextPending()?.toQueueItem()
    }
    
    override suspend fun updateStatus(item: PrintingQueueItem, status: QueueItemStatus) {
        dao.updateStatus(item.id, status.name)
    }
    
    override suspend fun remove(item: PrintingQueueItem) {
        dao.delete(item.id)
    }

    override suspend fun removeByStatus(statuses: List<QueueItemStatus>) {
        dao.deleteByStatus(statuses.map { it.name })
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<PrintingQueueItem> {
        return dao.getAllByStatus(
            statuses.map { it.name }
        ).map { it.toQueueItem() }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<PrintingQueueItem>> {
        return dao.observeByStatus(status.name).map { entities -> 
            entities.map { it.toQueueItem() }
        }
    }
}
