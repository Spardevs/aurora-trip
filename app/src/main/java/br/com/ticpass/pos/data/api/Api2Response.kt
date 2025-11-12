package br.com.ticpass.pos.data.api

data class LoginResponse(
    val user: UserV2,
    val jwt: Jwt
)

data class UserV2(
    val id: String,
    val avatar: Int,
    val username: String,
    val name: String,
    val email: String,
    val totp: Boolean,
    val verifiedAt: String?,
    val oauth2: List<String>,
    val managers: List<String>,
    val role: String,
    val notificationPreferences: NotificationPreferences,
    val accountable: String,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)

data class NotificationPreferences(
    val charges: List<String>,
    val goals: List<String>,
    val menuChanges: List<String>,
    val news: List<String>
)

data class Jwt(
    val access: String,
    val refresh: String
)

data class ErrorResponse(
    val statusCode: Int,
    val error: String,
    val message: String
)