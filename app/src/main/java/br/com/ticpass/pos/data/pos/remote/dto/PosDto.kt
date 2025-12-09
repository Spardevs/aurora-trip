package br.com.ticpass.pos.data.pos.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosResponseDto(
    val edges: List<PosDto>,
    val info: PageInfoDto
) {
    fun hasNextPage(): Boolean = info.hasNextPage
    fun getNextPage(): Int? = info.nextPage
}

@JsonClass(generateAdapter = true)
data class PosDto(
    val id: String,
    val prefix: String,
    val sequence: Int,
    val mode: String,
    val commission: Long,
    val menu: String,
    val session: SessionDto?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class SessionDto(
    val id: String,
    val accountable: String?,
    val device: String?,
    val menu: String?,
    val pos: String?,
    val cashier: CashierDto,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CashierDto(
    val id: String?,
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

@JsonClass(generateAdapter = true)
data class PageInfoDto(
    val total: Int,
    val limit: Int,
    val page: Int,
    val pageCount: Int,
    val hasNextPage: Boolean,
    val nextPage: Int?,
    val hasPrevPage: Boolean,
    val prevPage: Int?,
    val cursor: Int
)