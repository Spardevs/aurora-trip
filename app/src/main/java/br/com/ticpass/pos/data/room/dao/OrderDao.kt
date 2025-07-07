package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.OrderEntity
import br.com.ticpass.pos.data.room.entity._OrderPopulated

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders")
    suspend fun getAllPopulated(): List<_OrderPopulated>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders ORDER BY ROWID DESC LIMIT 1")
    fun getLast(): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("""
        DELETE FROM orders 
        WHERE id NOT IN (
            SELECT id FROM (
                SELECT id, createdAt FROM orders 
                WHERE synced = 0 
                UNION ALL 
                SELECT id, createdAt FROM orders 
                WHERE synced = 1 
                ORDER BY createdAt DESC 
                LIMIT :keep
            ) AS subquery
        )
    """)
    suspend fun deleteOld(keep: Int)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE synced = :syncState")
    suspend fun getBySyncState(syncState: Boolean): List<OrderEntity>

    @Update
    suspend fun updateMany(orders: List<OrderEntity>)

    @Update
    suspend fun setManySync(orders: List<OrderEntity>)

    @Query("DELETE FROM orders")
    suspend fun clearOrders()
}
