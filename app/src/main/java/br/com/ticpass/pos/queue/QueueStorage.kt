package br.com.ticpass.pos.queue

import kotlinx.coroutines.flow.Flow

/**
 * Generic Queue Storage Interface
 * Defines how queue items are stored and retrieved
 */
interface QueueStorage<T : QueueItem> {
    suspend fun insert(item: T)
    suspend fun getNextPending(): T?
    suspend fun updateStatus(item: T, status: QueueItemStatus)
    suspend fun remove(item: T)
    suspend fun getAllByStatus(status: QueueItemStatus): List<T>
    fun observeByStatus(status: QueueItemStatus): Flow<List<T>>
}
