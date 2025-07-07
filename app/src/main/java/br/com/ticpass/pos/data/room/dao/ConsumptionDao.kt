package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.ConsumptionEntity
import br.com.ticpass.pos.data.room.entity._ConsumptionPopulated

@Dao
interface ConsumptionDao {
    @Query("""
           DELETE FROM consumptions 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM consumptions 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM consumptions 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM consumptions WHERE id IN (:ids)")
    suspend fun getManyById(ids: List<String>): List<_ConsumptionPopulated>

    @Query("SELECT * FROM consumptions")
    fun getAll(): List<ConsumptionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(consumptions: List<ConsumptionEntity>)

    @Query("SELECT * FROM consumptions WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<ConsumptionEntity>

    @Update
    suspend fun updateMany(consumptions: List<ConsumptionEntity>)
}
