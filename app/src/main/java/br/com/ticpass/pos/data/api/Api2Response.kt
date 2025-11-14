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

data class RegisterDeviceRequest(
    val serial: String,
    val acquirer: String,
    val variant: String
)

data class RegisterDeviceResponse(
    val id: String,
    val serial: String,
    val acquirer: String,
    val variant: String,
    val createdAt: String,
    val updatedAt: String
)

// Modelos do /menu
data class MenuListResponse(
    val edges: List<MenuEdge>,
    val info: PageInfo
)

data class MenuEdge(
    val id: String,
    val label: String,
    val status: String,
    val mode: String,
    val logo: String?,    // ✅ NOVO: ID da logo
    val goal: Long,
    val date: MenuDate,
    val pass: MenuPass,
    val payment: MenuPayment,
    val team: List<String>,
    val accountable: Accountable,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

data class MenuDate(
    val start: String,
    val end: String
)

data class MenuPass(
    val vouchering: Boolean,
    val pricing: Boolean,
    val mode: String,
    val description: String
)

data class MenuPayment(
    val methods: List<String>,
    val multi: Boolean,
    val acquirer: Boolean
)

data class Accountable(
    val id: String,
    val name: String
)

data class PageInfo(
    val total: Int,
    val limit: Int,
    val page: Int,
    val pageCount: Int,
    val hasNextPage: Boolean,
    val nextPage: Any?,
    val hasPrevPage: Boolean,
    val prevPage: Any?,
    val cursor: Int
)

// Modelos do /menu-pos
data class MenuPosListResponse(
    val edges: List<PosEdge>,
    val info: PageInfo
)

data class PosEdge(
    val id: String,
    val prefix: String,
    val sequence: Int,
    val mode: String,
    val commission: Int,
    val menu: String,
    val session: PosSession?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

data class PosSession(
    val id: String,
    val accountable: String,
    val device: String,
    val menu: String,
    val pos: String,
    val cashier: PosCashier,
    val createdAt: String
)

data class PosCashier(
    val id: String,
    val avatar: Int,
    val username: String,
    val name: String,
    val email: String,
    val role: String,
    val totp: Boolean,
    val managers: List<String>,
    val oauth2: List<String>,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)