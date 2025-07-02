package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category")
    fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE id = :id")
    fun findById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: CategoryEntity)

    @Delete
    fun delete(entity: CategoryEntity)
}