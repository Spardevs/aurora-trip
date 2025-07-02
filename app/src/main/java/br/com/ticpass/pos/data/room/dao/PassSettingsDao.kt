package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.PassSettingsEntity

@Dao
interface PassSettingsDao {
    @Query("SELECT * FROM pass_settings")
    fun getAll(): List<PassSettingsEntity>

    @Query("SELECT * FROM pass_settings WHERE id = :id")
    fun findById(id: Int): PassSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PassSettingsEntity)

    @Delete
    fun delete(entity: PassSettingsEntity)
}