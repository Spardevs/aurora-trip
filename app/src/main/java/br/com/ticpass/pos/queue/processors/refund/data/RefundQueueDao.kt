package br.com.ticpass.pos.queue.processors.refund.data

import androidx.room.*
import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Refund Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface RefundQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(refund: RefundQueueEntity)

    @Update
    suspend fun update(refund: RefundQueueEntity): Unit

    @Query("SELECT * FROM refund_queue WHERE status = :status ORDER BY priority DESC LIMIT 1")
    suspend fun getNextPending(status: String = QueueItemStatus.PENDING.name): RefundQueueEntity?

    @Query("UPDATE refund_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM refund_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM refund_queue WHERE status IN (:statuses)")
    suspend fun deleteByStatus(statuses: List<String>)

    @Query("SELECT * FROM refund_queue WHERE status IN (:statuses) ORDER BY priority DESC")
    suspend fun getAllByStatus(statuses: List<String>): List<RefundQueueEntity>

    @Query("SELECT * FROM refund_queue WHERE status = :status ORDER BY priority DESC")
    fun observeByStatus(status: String): Flow<List<RefundQueueEntity>>
}
