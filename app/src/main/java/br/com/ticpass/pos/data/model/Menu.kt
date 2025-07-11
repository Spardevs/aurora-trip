package br.com.ticpass.pos.data.model

import java.sql.Date

data class Menu(
    val id: String,
    val name: String,
    val imageUrl: String,
    val dateStart: String,
    val dateEnd: String,
    val details: String = "",
    val pin: String,
    val mode: String
)