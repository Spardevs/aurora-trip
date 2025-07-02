package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity

@Dao
interface AcquisitionDao {
    @Query("SELECT * FROM acquisition")
    fun getAll(): List<AcquisitionEntity>

    @Query("SELECT * FROM acquisition WHERE id = :id")
    fun findById(id: String): AcquisitionEntity?

    @Query("SELECT * FROM acquisition WHERE orderId = :orderId")
    fun findByOrder(orderId: String): List<AcquisitionEntity>

    @Query("SELECT * FROM acquisition WHERE passId = :passId")
    fun findByPass(passId: String): List<AcquisitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: AcquisitionEntity)

    @Delete
    fun delete(entity: AcquisitionEntity)
}