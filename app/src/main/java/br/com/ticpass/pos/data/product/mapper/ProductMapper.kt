package br.com.ticpass.pos.data.product.mapper

import br.com.ticpass.pos.data.product.local.entity.ProductEntity
import br.com.ticpass.pos.data.product.remote.dto.ProductDto
import br.com.ticpass.pos.domain.product.model.ProductModel


fun ProductDto.toEntity(): ProductEntity {
    val categoryId = when (val cat = this.category) {
        is String -> cat
        else -> ""
    }

    return ProductEntity(
        id = this.id,
        menuId = this.menu,
        category = categoryId,
        name = this.label,
        thumbnail = this.thumbnail?.id ?: "",
        price = this.price.toLong(),
        stock = 0,
        isEnabled = true,
        menuProductId = this.menuProductId
    )
}

fun ProductEntity.toDomain(): ProductModel =
    ProductModel(
        id = id,
        category = category,
        name = name,
        thumbnail = thumbnail,
        price = price,
        stock = stock,
        isEnabled = isEnabled,
        menuProductId = menuProductId
    )