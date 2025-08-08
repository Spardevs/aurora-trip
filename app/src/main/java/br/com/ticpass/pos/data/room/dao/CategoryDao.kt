package br.com.ticpass.pos.data.room.dao



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.entity.CategoryWithEnabledProducts
import br.com.ticpass.pos.data.room.entity.CategoryWithProducts

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Transaction
    @Query("SELECT * FROM categories")
    suspend fun getCategoriesWithProducts(): List<CategoryWithProducts>
    @Transaction
    @Query("SELECT * FROM categories")
    suspend fun getCategoriesWithEnabledProducts(): List<CategoryWithEnabledProducts>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(events: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE id IN (:categoryIds)")
    suspend fun getByIds(categoryIds: List<String>): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
