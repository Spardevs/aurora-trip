package br.com.ticpass.pos.data.room.dao

import androidx.room.*
import br.com.ticpass.pos.data.room.entity.PaymentEntity

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment")
    fun getAll(): List<PaymentEntity>

    @Query("SELECT * FROM payment WHERE id = :id")
    fun findById(id: String): PaymentEntity?

    @Query("SELECT * FROM payment WHERE orderId = :orderId")
    fun findByOrder(orderId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PaymentEntity)

    @Delete
    fun delete(entity: PaymentEntity)
}