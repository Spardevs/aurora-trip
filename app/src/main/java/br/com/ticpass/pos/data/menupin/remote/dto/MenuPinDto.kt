package br.com.ticpass.pos.data.menupin.remote.dto

/**
 * DTO for menu pin summary response from API
 * Endpoint: GET /menu-pin-summary/{menuId}
 */
data class MenuPinDto(
    val id: String,
    val code: String,
    val user: MenuPinUserDto,
    val menu: String,
    val createdAt: String,
    val updatedAt: String
)

data class MenuPinUserDto(
    val id: String,
    val avatar: Int?,
    val username: String?,
    val name: String?,
    val email: String?
)
