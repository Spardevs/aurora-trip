package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.RefundEntity
import br.com.ticpass.pos.data.room.entity._RefundPopulated

@Dao
interface RefundDao {
    @Query("""
           DELETE FROM refunds 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM refunds 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM refunds 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM refunds WHERE id IN (:ids)")
    suspend fun getManyById(ids: List<String>): List<_RefundPopulated>

    @Query("SELECT * FROM refunds")
    fun getAll(): List<RefundEntity>

    @Query("SELECT * FROM refunds WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<RefundEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(refunds: List<RefundEntity>)

    @Update
    suspend fun updateMany(refunds: List<RefundEntity>)
}
