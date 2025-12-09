package br.com.ticpass.pos.data.user.repository

import br.com.ticpass.pos.data.user.local.entity.UserEntity

/**
 * Repository interface for user data operations.
 */
interface UserRepository {
    /**
     * Get the currently logged in user.
     * @return The logged in user entity, or null if no user is logged in.
     */
    suspend fun getLoggedUser(): UserEntity?
}
