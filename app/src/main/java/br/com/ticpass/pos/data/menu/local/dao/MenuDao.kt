package br.com.ticpass.pos.data.menu.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticpass.pos.data.menu.local.entity.MenuEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenus(menus: List<MenuEntity>)

    @Query("SELECT * FROM menus")
    fun getAllMenus(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE id = :id")
    suspend fun getMenuById(id: String): MenuEntity?

    @Query("DELETE FROM menus")
    suspend fun clearAllMenus()

    @Query("UPDATE menus SET isSelected = :selected WHERE id = :id")
    suspend fun selectMenu(id: String, selected: Boolean)

    @Query("SELECT * FROM menus WHERE isSelected = :selected")
    suspend fun getSelectedMenu(selected: Boolean): MenuEntity?
}