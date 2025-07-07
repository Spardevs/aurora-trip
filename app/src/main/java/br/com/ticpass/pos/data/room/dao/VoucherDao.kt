package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.VoucherEntity
import br.com.ticpass.pos.data.room.entity._VoucherPopulated

@Dao
interface VoucherDao {
    @Query("""
           DELETE FROM vouchers 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM vouchers 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM vouchers 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM vouchers WHERE id IN (:ids)")
    suspend fun getManyById(ids: List<String>): List<_VoucherPopulated>

    @Query("SELECT * FROM vouchers")
    fun getAll(): List<VoucherEntity>

    @Query("SELECT * FROM vouchers WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<VoucherEntity>

    @Update
    suspend fun updateMany(vouchers: List<VoucherEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(vouchers: List<VoucherEntity>)
}
