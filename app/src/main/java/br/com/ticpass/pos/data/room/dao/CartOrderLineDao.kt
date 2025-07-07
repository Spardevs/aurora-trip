package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity._CartOrderLineEntity

@Dao
interface CartOrderLineDao {
    @Query("SELECT * FROM cartOrderLines")
    suspend fun getAll(): List<_CartOrderLineEntity>

    @Query("SELECT * FROM cartOrderLines WHERE product = :productId")
    suspend fun getByProductId(productId: String): _CartOrderLineEntity?

    @Query("SELECT * FROM cartOrderLines WHERE product = :productId")
    suspend fun getManyByProductId(productId: List<String>): List<_CartOrderLineEntity>

    @Update
    suspend fun updateCartOrderLine(cartOrderLine: _CartOrderLineEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(cartOrderLines: List<_CartOrderLineEntity>)

    @Query("DELETE FROM cartOrderLines WHERE product = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("DELETE FROM cartOrderLines")
    suspend fun clearAll()
}
