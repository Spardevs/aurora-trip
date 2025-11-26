package br.com.ticpass.pos.data.auth.remote.dto

data class LoginResponse(
    val user: UserDto,
    val jwt: JwtDto
)

data class UserDto(
    val id: String,
    val avatar: Int? = null,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val totp: Boolean? = null,
    val verifiedAt: String? = null,
    val oauth2: List<Any>? = null,
    val managers: List<Any>? = null,
    val role: String? = null,
    val notificationPreferences: NotificationPreferencesDto? = null,
    val accountable: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
)

data class NotificationPreferencesDto(
    val charges: List<String>? = null,
    val goals: List<String>? = null,
    val menuChanges: List<String>? = null,
    val news: List<String>? = null
)

data class JwtDto(
    val access: String? = null,
    val refresh: String? = null
)