package br.com.ticpass.pos.domain.pos.model

data class Pos(
    val id: String,
    val prefix: String,
    val sequence: Int,
    val mode: String,
    val commission: Long,
    val menu: String,
    val session: Session?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

data class Session(
    val id: String,
    val accountable: String,
    val device: String,
    val menu: String,
    val pos: String,
    val cashier: Cashier?,
    val createdAt: String
)

data class Cashier(
    val id: String,
    val avatar: Int?,
    val username: String?,
    val name: String?,
    val email: String?,
    val role: String?,
    val totp: Boolean?,
    val managers: List<String> = emptyList(),
    val oauth2: List<String> = emptyList(),
    val createdBy: String?,
    val createdAt: String?,
    val updatedAt: String?
)