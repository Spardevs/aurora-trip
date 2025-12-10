package br.com.ticpass.pos.data.menupin.mapper

import br.com.ticpass.pos.data.menupin.local.entity.MenuPinEntity
import br.com.ticpass.pos.data.menupin.remote.dto.MenuPinDto
import br.com.ticpass.pos.domain.menupin.model.MenuPin
import br.com.ticpass.pos.domain.menupin.model.MenuPinUser

/**
 * Extension functions for mapping between MenuPin layers
 */

// DTO -> Entity
fun MenuPinDto.toEntity(): MenuPinEntity {
    return MenuPinEntity(
        id = id,
        code = code,
        menuId = menu,
        userId = user.id,
        userName = user.name,
        userEmail = user.email,
        userAvatar = user.avatar,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Entity -> Domain
fun MenuPinEntity.toDomain(): MenuPin {
    return MenuPin(
        id = id,
        code = code,
        menuId = menuId,
        user = MenuPinUser(
            id = userId,
            name = userName,
            email = userEmail,
            avatar = userAvatar
        ),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Domain -> Entity
fun MenuPin.toEntity(): MenuPinEntity {
    return MenuPinEntity(
        id = id,
        code = code,
        menuId = menuId,
        userId = user.id,
        userName = user.name,
        userEmail = user.email,
        userAvatar = user.avatar,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
