package br.com.ticpass.pos.queue.processors.printing.data

import androidx.room.*
import br.com.ticpass.pos.queue.processors.printing.models.PrintEntity
import kotlinx.coroutines.flow.Flow

/**
 * Print Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface PrintQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(printEntity: PrintEntity)

    @Update
    suspend fun update(printEntity: PrintEntity)

    @Query("SELECT * FROM print_queue WHERE status = 'pending' ORDER BY priority DESC")
    suspend fun getNextPending(): PrintEntity?

    @Query("UPDATE print_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM print_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM payment_queue WHERE status IN (:statuses)")
    suspend fun deleteByStatus(statuses: List<String>)

    @Query("SELECT * FROM print_queue WHERE status = :status ORDER BY priority DESC")
    suspend fun getAllByStatus(status: String): List<PrintEntity>

    @Query("SELECT * FROM print_queue WHERE status = :status ORDER BY priority DESC")
    fun observeByStatus(status: String): Flow<List<PrintEntity>>
}
