package br.com.ticpass.pos.data.room.dao



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticpass.pos.data.room.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(events: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE id IN (:categoryIds)")
    suspend fun getByIds(categoryIds: List<String>): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
