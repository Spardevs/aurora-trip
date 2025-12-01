package br.com.ticpass.pos.data.pos.local.dao

import androidx.room.*
import br.com.ticpass.pos.data.pos.local.entity.PosEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    @Query("SELECT * FROM pos")
    fun getAllPos(): Flow<List<PosEntity>>

    @Query("SELECT * FROM pos WHERE id = :id")
    suspend fun getPosById(id: String): PosEntity?

    @Query("SELECT * FROM pos WHERE menu = :menuId")
    fun getPosByMenu(menuId: String): Flow<List<PosEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posList: List<PosEntity>)

    @Update
    suspend fun update(pos: PosEntity)

    @Delete
    suspend fun delete(pos: PosEntity)

    @Query("DELETE FROM pos")
    suspend fun deleteAll()

    @Query("UPDATE pos SET isSelected = :selected WHERE id = :id")
    suspend fun selectPos(id: String, selected: Boolean)

    @Query("SELECT * FROM pos WHERE isSelected = :selected")
    suspend fun getSelectedPos(selected: Boolean): PosEntity?
}