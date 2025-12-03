package br.com.ticpass.pos.domain.user.repository

import br.com.ticpass.pos.data.user.local.entity.UserEntity

interface UserRepository {
    suspend fun getLoggedUser(): UserEntity?
    suspend fun isLoggedIn(): Boolean
}