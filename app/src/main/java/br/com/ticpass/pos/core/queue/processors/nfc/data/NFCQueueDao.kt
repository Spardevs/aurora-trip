package br.com.ticpass.pos.core.queue.processors.nfc.data

import androidx.room.*
import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * NFC Queue DAO
 * Data Access Object for Room database operations
 */
@Dao
interface NFCQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(nfc: NFCQueueEntity)

    @Update
    suspend fun update(nfc: NFCQueueEntity): Unit

    @Query("SELECT * FROM nfc_queue WHERE status = :status ORDER BY priority DESC LIMIT 1")
    suspend fun getNextPending(status: String = QueueItemStatus.PENDING.name): NFCQueueEntity?

    @Query("UPDATE nfc_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM nfc_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM nfc_queue WHERE status IN (:statuses)")
    suspend fun deleteByStatus(statuses: List<String>)

    @Query("SELECT * FROM nfc_queue WHERE status IN (:statuses) ORDER BY priority DESC")
    suspend fun getAllByStatus(statuses: List<String>): List<NFCQueueEntity>

    @Query("SELECT * FROM nfc_queue WHERE status = :status ORDER BY priority DESC")
    fun observeByStatus(status: String): Flow<List<NFCQueueEntity>>
}
