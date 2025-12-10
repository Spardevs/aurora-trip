package br.com.ticpass.pos.domain.menupin.model

/**
 * Domain model for menu pin (whitelist entry for a menu)
 */
data class MenuPin(
    val id: String,
    val code: String,
    val menuId: String,
    val user: MenuPinUser,
    val createdAt: String,
    val updatedAt: String
)

data class MenuPinUser(
    val id: String,
    val name: String?,
    val email: String?,
    val avatar: Int?
)
