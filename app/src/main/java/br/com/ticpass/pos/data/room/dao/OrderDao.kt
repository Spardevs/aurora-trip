package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.OrderEntity

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders")
    fun getAll(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun findById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: OrderEntity)

    @Delete
    fun delete(entity: OrderEntity)
}