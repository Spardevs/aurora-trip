package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.com.ticpass.pos.data.room.entity._VoucherExchangeProductPopulated

@Dao
interface VoucherExchangeProductDao {
    @Query("""
           DELETE FROM voucherExchangeProducts 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM voucherExchangeProducts 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM voucherExchangeProducts")
    fun getAll(): List<_VoucherExchangeProductPopulated>
}
