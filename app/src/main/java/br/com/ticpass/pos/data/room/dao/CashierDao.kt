package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.CashierEntity

@Dao
interface CashierDao {

    @Query("SELECT * FROM cashiers LIMIT 1")
    suspend fun getUser(): CashierEntity?

    @Update
    suspend fun updateUser(cashier: CashierEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(cashier: CashierEntity): Long

    @Delete
    suspend fun removeUser(cashier: CashierEntity)
}
