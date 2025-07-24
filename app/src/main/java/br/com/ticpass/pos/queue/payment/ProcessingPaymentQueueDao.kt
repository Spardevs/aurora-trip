package br.com.ticpass.pos.queue.payment

import androidx.room.*
import br.com.ticpass.pos.queue.QueueItemStatus
import kotlinx.coroutines.flow.Flow

/**
 * Payment Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface ProcessingPaymentQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: ProcessingPaymentEntity)

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC LIMIT 1")
    suspend fun getNextPending(status: String = QueueItemStatus.PENDING.name): ProcessingPaymentEntity?

    @Query("UPDATE payment_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM payment_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC")
    suspend fun getAllByStatus(status: String): List<ProcessingPaymentEntity>

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC")
    fun observeByStatus(status: String): Flow<List<ProcessingPaymentEntity>>
}
