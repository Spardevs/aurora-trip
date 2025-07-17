package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.CategoryWithProducts
import br.com.ticpass.pos.data.room.entity.ProductEntity

@Dao
interface ProductDao {

    @Transaction
    @Query("SELECT * FROM categories")
    suspend fun getCategoryWithProducts(): List<CategoryWithProducts>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getById(productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:productIds)")
    suspend fun getByIds(productIds: List<String>): List<ProductEntity>

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}