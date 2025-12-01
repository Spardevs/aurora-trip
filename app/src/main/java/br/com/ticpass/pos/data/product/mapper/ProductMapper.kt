package br.com.ticpass.pos.data.product.mapper

import br.com.ticpass.pos.data.product.local.entity.ProductEntity
import br.com.ticpass.pos.data.product.remote.dto.ProductDto


fun ProductDto.toEntity(): ProductEntity {
    return ProductEntity(
        id = this.id,
        category = this.category?.id ?: "",
        name = this.label,
        thumbnail = this.thumbnail.id,
        price = this.price,
        stock = 0,
        isEnabled = true
    )
}