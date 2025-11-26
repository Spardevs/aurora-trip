package br.com.ticpass.pos.data.user.mapper


import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse
import br.com.ticpass.pos.data.user.local.entity.UserEntity

fun LoginResponse.toUserEntity(): UserEntity {
    return UserEntity(
        id = this.user.id,
        accessToken = this.jwt.access,   // pode ser null
        refreshToken = this.jwt.refresh  // pode ser null
    )
}