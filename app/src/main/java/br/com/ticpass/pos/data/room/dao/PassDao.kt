package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.PassEntity

@Dao
interface PassDao {
    @Query("SELECT * FROM pass")
    fun getAll(): List<PassEntity>

    @Query("SELECT * FROM pass WHERE id = :id")
    fun findById(id: String): PassEntity?

    @Query("SELECT * FROM pass WHERE orderId = :orderId")
    fun findByOrder(orderId: String): List<PassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PassEntity)

    @Delete
    fun delete(entity: PassEntity)
}