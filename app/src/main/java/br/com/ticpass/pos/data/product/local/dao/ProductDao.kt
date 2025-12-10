package br.com.ticpass.pos.data.product.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticpass.pos.data.product.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM product WHERE isEnabled = 1")
    fun getEnabledProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?
}