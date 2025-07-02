package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.PosEntity

@Dao
interface PosDao {
    @Query("SELECT * FROM pos")
    fun getAll(): List<PosEntity>

    @Query("SELECT * FROM pos WHERE id = :id")
    fun findById(id: String): PosEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PosEntity)

    @Delete
    fun delete(entity: PosEntity)
}