package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.entity._AcquisitionPopulated

@Dao
interface AcquisitionDao {
    @Query("""
           DELETE FROM acquisitions 
           WHERE id NOT IN (
               SELECT id FROM (
                   SELECT id, createdAt FROM acquisitions 
                   WHERE synced = 0 
                   UNION ALL 
                   SELECT id, createdAt FROM acquisitions 
                   WHERE synced = 1 
                   ORDER BY createdAt DESC 
                   LIMIT :keep
               ) AS subquery
           )
       """)
    suspend fun deleteOld(keep: Int)

    @Transaction
    @Query("SELECT * FROM acquisitions WHERE id IN (:ids)")
    suspend fun getManyById(ids: List<String>): List<_AcquisitionPopulated>

    @Query("SELECT * FROM acquisitions WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<AcquisitionEntity>

    @Transaction
    @Query("SELECT * FROM acquisitions WHERE pass IN (:ids)")
    suspend fun getManyByPassId(ids: List<String>): List<AcquisitionEntity>

    @Transaction
    @Query("SELECT * FROM acquisitions")
    suspend fun getAll(): List<_AcquisitionPopulated>

    @Query("SELECT COALESCE(SUM(price), 0) FROM acquisitions")
    fun getRevenue(): Long

    @Query("SELECT COALESCE(SUM(CASE WHEN refund IS NOT NULL AND refund <> '' THEN price ELSE 0 END), 0) FROM acquisitions")
    fun getRefundAmount(): Long

    @Transaction
    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE `refund` = :id"
    )
    fun getAllByRefundId(id: String): List<AcquisitionEntity>

    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE `voucher` = :id"
    )
    fun getAllByVoucherId(id: String): List<AcquisitionEntity>

    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE `consumption` = :id"
    )
    fun getAllByConsumptionId(id: String): List<AcquisitionEntity>

    @Transaction
    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE (refund IS NULL OR refund = '') " +
                "AND (voucher IS NULL OR voucher = '') " +
                "AND (consumption IS NULL OR consumption = '')"
    )
    fun getAllAvailable(): List<AcquisitionEntity>

    @Transaction
    @Query("SELECT * FROM acquisitions WHERE refund IS NOT NULL AND refund <> ''")
    fun getAllRefunds(): List<AcquisitionEntity>

    @Transaction
    @Query("SELECT * FROM acquisitions WHERE voucher IS NOT NULL AND voucher <> ''")
    fun getAllVouchered(): List<AcquisitionEntity>

    @Transaction
    @Query("SELECT * FROM acquisitions WHERE consumption IS NOT NULL AND consumption <> ''")
    fun getAllConsumed(): List<AcquisitionEntity>

    @Transaction
    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE `order` = :orderId"
    )
    suspend fun getByOrderId(orderId: String): List<_AcquisitionPopulated>

    @Transaction
    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE `order` = :orderId " +
                "AND (voucher IS NULL OR voucher = '') " +
                "AND (refund IS NULL OR refund = '') " +
                "AND (consumption IS NULL OR consumption = '')"
    )
    suspend fun getUnconsumedByOrderId(orderId: String): List<AcquisitionEntity>

    @Transaction
    @Query(
        "SELECT * FROM acquisitions " +
                "WHERE `order` = :orderId " +
                "AND ((voucher IS NOT NULL AND voucher <> '') " +
                "OR (refund IS NOT NULL AND refund <> '') " +
                "OR (consumption IS NOT NULL AND consumption <> ''))"
    )
    suspend fun getConsumedByOrderId(orderId: String): List<_AcquisitionPopulated>


    @Update
    suspend fun updateMany(acquisitions: List<AcquisitionEntity>)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(passes: List<AcquisitionEntity>)
}
