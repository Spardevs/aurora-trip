package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.PassEntity
import br.com.ticpass.pos.data.room.entity._PassPopulated

@Dao
interface PassDao {
    @Query("""
           DELETE FROM passes 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM passes 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM passes 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM passes")
    suspend fun getAll(): List<_PassPopulated>

    @Query("SELECT * FROM passes WHERE id = :id")
    suspend fun getById(id: String): PassEntity?

    @Query("SELECT * FROM passes WHERE `order` = :orderId")
    suspend fun getByOrderId(orderId: String): PassEntity?

    @Query("SELECT * FROM passes WHERE `order` = :orderId")
    suspend fun getManyByOrderId(orderId: String): List<PassEntity>

    @Query("SELECT * FROM passes WHERE id IN (:ids)")
    fun getManyById(ids: List<String>): List<PassEntity>

    @Transaction
    @Query("SELECT * FROM passes WHERE id IN (:ids)")
    fun getManyPopulatedById(ids: List<String>): List<_PassPopulated>

    @Query("SELECT * FROM passes WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<PassEntity>

    @Query("SELECT * FROM passes WHERE printingRetries BETWEEN :min AND :max")
    suspend fun getAllByPrintingRetries(min: Int, max: Int): List<PassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMany(passes: List<PassEntity>)

    @Update
    suspend fun updateMany(passes: List<PassEntity>)

    @Update
    suspend fun updatePass(pass: PassEntity)
}
