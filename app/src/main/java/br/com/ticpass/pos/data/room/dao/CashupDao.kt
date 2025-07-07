package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.CashupEntity

@Dao
interface CashupDao {

    @Query("SELECT * FROM cashups")
    suspend fun getAllCashups(): List<CashupEntity>

    @Query("SELECT * FROM cashups WHERE id = :cashupId")
    suspend fun getCashupById(cashupId: String): CashupEntity?

    @Query("SELECT COALESCE(SUM(taken), 0) FROM cashups")
    fun getTaken(): Long

    @Query("""
           DELETE FROM cashups 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM cashups 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM cashups 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCashups(cashups: List<CashupEntity>)

    @Query("SELECT * FROM cashups WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<CashupEntity>

    @Update
    suspend fun updateMany(cashups: List<CashupEntity>)

    @Query("DELETE FROM cashups")
    suspend fun clearCashups()
}
