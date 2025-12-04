package br.com.ticpass.pos.data.pos.mapper

import br.com.ticpass.pos.data.pos.local.entity.PosEntity
import br.com.ticpass.pos.data.pos.remote.dto.*

import br.com.ticpass.pos.domain.pos.model.*

fun PosDto.toEntity(): PosEntity = PosEntity(
    id = id,
    prefix = prefix,
    sequence = sequence,
    mode = mode,
    commission = commission,
    menu = menu,
    sessionId = session?.id,
    cashierId = session?.cashier?.id,
    cashierName = session?.cashier?.name
)

fun PosEntity.toDomain(): Pos = Pos(
    id = id,
    prefix = prefix,
    sequence = sequence,
    mode = mode,
    commission = commission,
    menu = menu,
    session = sessionId?.let { sid ->
        Session(
            id = sid,
            accountable = "",
            device = "",
            menu = menu ?: "",
            pos = id,
            cashier = if (cashierId != null || cashierName != null) {
                Cashier(
                    id = cashierId ?: "",
                    avatar = null,
                    username = null,
                    name = cashierName ?: "",
                    email = null,
                    role = null,
                    totp = null,
                    managers = emptyList(),
                    oauth2 = emptyList(),
                    createdBy = null,
                    createdAt = null,
                    updatedAt = null
                )
            } else null,
        )
    }
)

fun PosDto.toDomain(): Pos = Pos(
    id = id,
    prefix = prefix,
    sequence = sequence,
    mode = mode,
    commission = commission,
    menu = menu,
    session = session?.toDomain()
)

fun SessionDto.toDomain(): Session = Session(
    id = id,
    accountable = accountable.toString(),
    device = device,
    menu = menu,
    pos = pos,
    cashier = try {
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter(CashierDto::class.java)
        val cashierDto = adapter.fromJsonValue(cashier)
        cashierDto?.toDomain()
    } catch (e: Exception) {
        Cashier(
            id = cashier.toString(),
            avatar = null,
            username = null,
            name = null,
            email = null,
            role = null,
            totp = null,
            managers = emptyList(),
            oauth2 = emptyList(),
            createdBy = null,
            createdAt = null,
            updatedAt = null
        )
    }
)

fun CashierDto.toDomain(): Cashier = Cashier(
    id = id.toString(),
    avatar = avatar,
    username = username,
    name = name,
    email = email,
    role = role,
    totp = totp,
    managers = managers,
    oauth2 = oauth2,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)