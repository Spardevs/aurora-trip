package br.com.ticpass.pos.data.menupin.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticpass.pos.data.menupin.local.entity.MenuPinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuPinDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pins: List<MenuPinEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pin: MenuPinEntity)
    
    @Query("SELECT * FROM menu_pin WHERE menuId = :menuId")
    fun getPinsByMenuId(menuId: String): Flow<List<MenuPinEntity>>
    
    @Query("SELECT * FROM menu_pin WHERE menuId = :menuId")
    suspend fun getPinsByMenuIdOnce(menuId: String): List<MenuPinEntity>
    
    @Query("SELECT * FROM menu_pin WHERE menuId = :menuId AND code = :code LIMIT 1")
    suspend fun getPinByCode(menuId: String, code: String): MenuPinEntity?
    
    @Query("SELECT * FROM menu_pin WHERE id = :id")
    suspend fun getPinById(id: String): MenuPinEntity?
    
    @Query("DELETE FROM menu_pin WHERE menuId = :menuId")
    suspend fun deleteByMenuId(menuId: String)
    
    @Query("DELETE FROM menu_pin")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM menu_pin WHERE menuId = :menuId")
    suspend fun countByMenuId(menuId: String): Int
}
