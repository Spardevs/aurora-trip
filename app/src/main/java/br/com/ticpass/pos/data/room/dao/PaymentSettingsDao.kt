package br.com.ticpass.pos.data.room.dao


import androidx.room.*
import br.com.ticpass.pos.data.room.entity.PaymentSettingsEntity

@Dao
interface PaymentSettingsDao {
    @Query("SELECT * FROM payment_settings")
    fun getAll(): List<PaymentSettingsEntity>

    @Query("SELECT * FROM payment_settings WHERE id = :id")
    fun findById(id: Int): PaymentSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PaymentSettingsEntity)

    @Delete
    fun delete(entity: PaymentSettingsEntity)
}