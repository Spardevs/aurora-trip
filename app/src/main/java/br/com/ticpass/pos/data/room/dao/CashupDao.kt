package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.CashupEntity

@Dao
interface CashupDao {
    @Query("SELECT * FROM cashup")
    fun getAll(): List<CashupEntity>

    @Query("SELECT * FROM cashup WHERE id = :id")
    fun findById(id: String): CashupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: CashupEntity)

    @Delete
    fun delete(entity: CashupEntity)
}