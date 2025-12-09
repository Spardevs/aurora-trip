package br.com.ticpass.pos.core.queue.processors.printing.data

import androidx.room.*
import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Printing Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface PrintingQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(printing: PrintingEntity)

    @Update
    suspend fun update(printing: PrintingEntity): Unit

    @Query("SELECT * FROM printing_queue WHERE status = :status ORDER BY priority DESC LIMIT 1")
    suspend fun getNextPending(status: String = QueueItemStatus.PENDING.name): PrintingEntity?

    @Query("UPDATE printing_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM printing_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM printing_queue WHERE status IN (:statuses)")
    suspend fun deleteByStatus(statuses: List<String>)

    @Query("SELECT * FROM printing_queue WHERE status IN (:statuses) ORDER BY priority DESC")
    suspend fun getAllByStatus(statuses: List<String>): List<PrintingEntity>

    @Query("SELECT * FROM printing_queue WHERE status = :status ORDER BY priority DESC")
    fun observeByStatus(status: String): Flow<List<PrintingEntity>>
}
