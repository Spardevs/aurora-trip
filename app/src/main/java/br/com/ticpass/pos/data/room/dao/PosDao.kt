package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.PosEntity

@Dao
interface PosDao {

    @Query("SELECT * FROM pos")
    suspend fun getAll(): List<PosEntity>

    @Query("UPDATE pos SET isSelected = 0")
    suspend fun deselectAllPos()

    @Query("SELECT * FROM pos WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedPos(): PosEntity

    @Query("SELECT * FROM pos WHERE id = :posId")
    suspend fun getById(posId: String): PosEntity?

    @Update
    suspend fun updatePosItem(posEntity: PosEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPosList(posList: List<PosEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPos(pos: PosEntity)

    @Delete
    suspend fun removePosItem(posEntity: PosEntity)

    @Query("DELETE FROM pos")
    suspend fun clearPosList()

    @Query("SELECT * FROM pos ORDER BY id LIMIT 1")
    suspend fun getFirstPos(): PosEntity?
}
