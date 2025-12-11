package br.com.ticpass.pos.data.category.mapper

import br.com.ticpass.pos.data.category.local.entity.CategoryEntity
import br.com.ticpass.pos.data.category.remote.dto.CategoryDto
import br.com.ticpass.pos.domain.category.model.Category

fun CategoryDto.toDomain(): Category {
    return Category(
        id = this.id,
        name = this.label
    )
}

fun Category.toEntity(menuId: String): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        menuId = menuId,
        name = this.name
    )
}

fun CategoryDto.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        menuId = this.menu,
        name = this.label
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = this.id,
        name = this.name
    )
}