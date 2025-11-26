package br.com.ticpass.pos.data.user.repository

import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.data.user.local.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getLoggedUser(): UserEntity? {
        return userDao.getAnyUserOnce()
    }
}