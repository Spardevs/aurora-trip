package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM product")
    fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM product WHERE id = :id")
    fun findById(id: String): ProductEntity?

    @Query("SELECT * FROM product WHERE categoryId = :categoryId")
    fun findByCategory(categoryId: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ProductEntity)

    @Delete
    fun delete(entity: ProductEntity)
}