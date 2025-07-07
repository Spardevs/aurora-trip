package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionEntity
import br.com.ticpass.pos.data.room.entity._VoucherRedemptionPopulated

@Dao
interface VoucherRedemptionDao {
    @Query("""
           DELETE FROM voucherRedemptions 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM voucherRedemptions 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM voucherRedemptions 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM voucherRedemptions")
    suspend fun getAll(): List<_VoucherRedemptionPopulated>

    @Update
    suspend fun updateMany(redemptions: List<VoucherRedemptionEntity>)

    @Query("SELECT * FROM voucherRedemptions WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<VoucherRedemptionEntity>
}
