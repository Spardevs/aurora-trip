package br.com.ticpass.pos.queue.processors.printing.data

import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.core.QueueStorage
import br.com.ticpass.pos.queue.processors.printing.models.PrintQueueItem
import br.com.ticpass.pos.queue.processors.printing.utils.toEntity
import br.com.ticpass.pos.queue.processors.printing.utils.toQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Print Storage
 * Implements QueueStorage interface using Room DAO
 */
class PrintStorage(private val printQueueDao: PrintQueueDao) : QueueStorage<PrintQueueItem> {
    
    override suspend fun insert(item: PrintQueueItem) {
        printQueueDao.insert(item.toEntity())
    }

    override suspend fun update(item: PrintQueueItem) {
        printQueueDao.update(item.toEntity())
    }

    override suspend fun getNextPending(): PrintQueueItem? {
        return printQueueDao.getNextPending()?.toQueueItem()
    }

    override suspend fun updateStatus(item: PrintQueueItem, status: QueueItemStatus) {
        printQueueDao.updateStatus(item.id, status.name)
    }

    override suspend fun remove(item: PrintQueueItem) {
        printQueueDao.delete(item.id)
    }

    override suspend fun getAllByStatus(status: QueueItemStatus): List<PrintQueueItem> {
        return printQueueDao.getAllByStatus(status.name).map { it.toQueueItem() }
    }

    override fun observeByStatus(status: QueueItemStatus): Flow<List<PrintQueueItem>> {
        return printQueueDao.observeByStatus(status.name).map { entities ->
            entities.map { it.toQueueItem() }
        }
    }
}
