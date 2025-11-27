package br.com.ticpass.pos.data.menu.mapper

import br.com.ticpass.pos.data.menu.local.entity.MenuEntity
import br.com.ticpass.pos.data.menu.remote.dto.MenuEdge
import br.com.ticpass.pos.domain.menu.model.Menu
import br.com.ticpass.pos.domain.menu.model.MenuDb

fun MenuEdge.toDomainModel(): Menu {
    return Menu(
        id = this.id,
        label = this.label,
        status = this.status,
        mode = this.mode,
        logo = this.logo,
        goal = this.goal,
        date = this.date,
        pass = this.pass,
        payment = this.payment,
        team = this.team,
        accountable = this.accountable,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun Menu.toEntity(): MenuEntity {
    return MenuEntity(
        id = this.id,
        name = this.label,                // label -> name
        logo = this.logo,
        pin = "",                         // não veio do remote; preenchimento padrão
        details = "",
        dateStart = this.date.start,
        dateEnd = this.date.end,
        mode = this.mode
    )
}

fun Menu.toMenuDb(): MenuDb {
    return MenuDb(
        id = this.id,
        name = this.label,
        logo = this.logo,
        pin = "",
        details = "",
        dateStart = this.date.start,
        dateEnd = this.date.end,
        mode = this.mode
    )
}

fun MenuDb.toEntity(): MenuEntity {
    return MenuEntity(
        id = this.id,
        name = this.name,
        logo = this.logo,
        pin = this.pin,
        details = this.details,
        dateStart = this.dateStart,
        dateEnd = this.dateEnd,
        mode = this.mode
    )
}

fun MenuEntity.toDomainModel(): MenuDb {
    return MenuDb(
        id = this.id,
        name = this.name,
        logo = this.logo,
        pin = this.pin,
        details = this.details,
        dateStart = this.dateStart,
        dateEnd = this.dateEnd,
        mode = this.mode
    )
}