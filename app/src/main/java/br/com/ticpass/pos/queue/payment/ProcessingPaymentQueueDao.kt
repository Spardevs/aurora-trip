package br.com.ticpass.pos.queue.payment

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Payment Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface ProcessingPaymentQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: ProcessingPaymentEntity)

    @Query("SELECT * FROM payment_queue WHERE status = 'pending' ORDER BY priority DESC, timestamp ASC LIMIT 1")
    suspend fun getNextPending(): ProcessingPaymentEntity?

    @Query("UPDATE payment_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM payment_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC, timestamp ASC")
    suspend fun getAllByStatus(status: String): List<ProcessingPaymentEntity>

    @Query("SELECT * FROM payment_queue WHERE status = :status ORDER BY priority DESC, timestamp ASC")
    fun observeByStatus(status: String): Flow<List<ProcessingPaymentEntity>>
}
