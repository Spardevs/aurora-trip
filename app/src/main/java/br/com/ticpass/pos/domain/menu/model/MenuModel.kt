package br.com.ticpass.pos.domain.menu.model

import br.com.ticpass.pos.data.menu.remote.dto.AccountableInfo
import br.com.ticpass.pos.data.menu.remote.dto.EventDate
import br.com.ticpass.pos.data.menu.remote.dto.PassInfo
import br.com.ticpass.pos.data.menu.remote.dto.PaymentInfo

data class Menu(
    val id: String,
    val label: String,
    val status: String,
    val mode: String,
    val logo: String?, // ID da logo para download
    val goal: Long,
    val date: EventDate,
    val pass: PassInfo,
    val payment: PaymentInfo,
    val team: List<String>,
    val accountable: AccountableInfo,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

data class EventDate(
    val start: String,
    val end: String
)

data class PassInfo(
    val vouchering: Boolean,
    val pricing: Boolean,
    val mode: String,
    val description: String
)

data class PaymentInfo(
    val methods: List<String>,
    val multi: Boolean,
    val acquirer: Boolean
)

data class AccountableInfo(
    val id: String,
    val name: String
)

data class MenuDb(
    val id: String,
    val name: String,
    val logo: String?, // logo path
    val pin: String,
    val details: String,
    val dateStart: String,
    val dateEnd: String,
    val mode: String
)