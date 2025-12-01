package br.com.ticpass.pos.data.user.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticpass.pos.data.user.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity) // não-nulo agora

    @Query("SELECT * FROM user WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getAnyUserOnce(): UserEntity?

    @Query("DELETE FROM user")
    suspend fun deleteAll()

    @Query("UPDATE user SET isLogged = :isLogged WHERE id = :userId")
    suspend fun updateUserLogged(userId: String, isLogged: Boolean)

    @Query("SELECT * FROM user WHERE isLogged = 1 LIMIT 1")
    suspend fun getLoggedUser(): UserEntity?
}