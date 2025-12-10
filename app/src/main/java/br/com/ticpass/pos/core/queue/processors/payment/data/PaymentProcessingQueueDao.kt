package br.com.ticpass.pos.core.queue.processors.payment.data

import androidx.room.*
import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Payment Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface PaymentProcessingQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentProcessingEntity)

    @Update
    suspend fun update(payment: PaymentProcessingEntity): Unit

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC LIMIT 1")
    suspend fun getNextPending(status: String = QueueItemStatus.PENDING.name): PaymentProcessingEntity?

    @Query("UPDATE payment_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM payment_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM payment_queue WHERE status IN (:statuses)")
    suspend fun deleteByStatus(statuses: List<String>)

    @Query("SELECT * FROM payment_queue WHERE status IN (:statuses) ORDER BY priority DESC")
    suspend fun getAllByStatus(statuses: List<String>): List<PaymentProcessingEntity>

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC")
    fun observeByStatus(status: String): Flow<List<PaymentProcessingEntity>>
}
