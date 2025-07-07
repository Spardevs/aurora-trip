package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.PaymentEntity

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments")
    suspend fun getAll(): List<PaymentEntity>

    @Query("SELECT COALESCE(SUM(commission), 0) FROM payments")
    suspend fun sumCommission(): Long

    @Query("""
        DELETE FROM payments 
        WHERE id NOT IN (
            SELECT id FROM (
                SELECT id, createdAt FROM payments 
                WHERE synced = 0 
                UNION ALL 
                SELECT id, createdAt FROM payments 
                WHERE synced = 1 
                ORDER BY createdAt DESC 
                LIMIT :keep
            ) AS subquery
        )
    """)
    suspend fun deleteOld(keep: Int)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE type = :paymentType")
    suspend fun sumAmountsByType(paymentType: String): Long

    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Query("SELECT * FROM payments WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id IN (:ids)")
    suspend fun getManyByIds(ids: List<String>): List<PaymentEntity>

    @Update
    suspend fun updateMany(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments")
    suspend fun clearPayments()
}
