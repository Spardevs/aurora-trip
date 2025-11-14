package br.com.ticpass.pos.data.model

data class Menu(
    val id: String,
    val name: String,
    val imageUrl: String,
    val dateStart: String,
    val dateEnd: String,
    val details: String,
    val mode: String,
    val pin: String,
    val logoId: String? = null          // ✅ NOVO: ID da logo
)