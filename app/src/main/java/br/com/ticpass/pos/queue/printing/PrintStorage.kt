package br.com.ticpass.pos.queue.printing

import br.com.ticpass.pos.queue.QueueStorage
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

    override suspend fun getNextPending(): PrintQueueItem? {
        return printQueueDao.getNextPending()?.toQueueItem()
    }

    override suspend fun updateStatus(item: PrintQueueItem, status: String) {
        printQueueDao.updateStatus(item.id, status)
    }

    override suspend fun remove(item: PrintQueueItem) {
        printQueueDao.delete(item.id)
    }

    override suspend fun getAllByStatus(status: String): List<PrintQueueItem> {
        return printQueueDao.getAllByStatus(status).map { it.toQueueItem() }
    }

    override fun observeByStatus(status: String): Flow<List<PrintQueueItem>> {
        return printQueueDao.observeByStatus(status).map { entities ->
            entities.map { it.toQueueItem() }
        }
    }
}
