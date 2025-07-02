package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.CartOrderLineEntity

@Dao
interface CartOrderLineDao {
    @Query("SELECT * FROM cart_order_line")
    fun getAll(): List<CartOrderLineEntity>

    @Query("SELECT * FROM cart_order_line WHERE product = :productId")
    fun findByProduct(productId: String): CartOrderLineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: CartOrderLineEntity)

    @Delete
    fun delete(entity: CartOrderLineEntity)
}