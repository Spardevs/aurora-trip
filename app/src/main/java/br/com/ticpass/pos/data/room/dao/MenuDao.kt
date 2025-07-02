package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.MenuEntity

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu")
    fun getAll(): List<MenuEntity>

    @Query("SELECT * FROM menu WHERE id = :id")
    fun findById(id: String): MenuEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(menu: MenuEntity)

    @Delete
    fun delete(menu: MenuEntity)
}