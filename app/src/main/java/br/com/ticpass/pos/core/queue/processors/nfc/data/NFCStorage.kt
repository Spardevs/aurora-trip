package br.com.ticpass.pos.core.queue.processors.nfc.data

import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.core.QueueStorage
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.utils.toEntity
import br.com.ticpass.pos.core.queue.processors.nfc.utils.toQueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * NFC Storage
 * Implements QueueStorage interface with Room database
 */
class NFCStorage(private val dao: NFCQueueDao) : QueueStorage<NFCQueueItem> {
    override suspend fun insert(item: NFCQueueItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun update(item: NFCQueueItem) {
        dao.update(item.toEntity())
    }
    
    override suspend fun getNextPending(): NFCQueueItem? {
        return dao.getNextPending()?.toQueueItem()
    }
    
    override suspend fun updateStatus(item: NFCQueueItem, status: QueueItemStatus) {
        dao.updateStatus(item.id, status.name)
    }
    
    override suspend fun remove(item: NFCQueueItem) {
        dao.delete(item.id)
    }

    override suspend fun removeByStatus(statuses: List<QueueItemStatus>) {
        dao.deleteByStatus(statuses.map { it.name })
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<NFCQueueItem> {
        return dao.getAllByStatus(
            statuses.map { it.name }
        ).map { it.toQueueItem() }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<NFCQueueItem>> {
        return dao.observeByStatus(status.name).map { entities -> 
            entities.map { it.toQueueItem() }
        }
    }
}
