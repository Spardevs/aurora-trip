package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.data.room.entity.ProductEntity

@Entity(tableName = "cartOrderLines")
data class _CartOrderLineEntity(
    @PrimaryKey val product: String,
    var count: Int,
)

@Entity
data class CartOrderLineEntity(
    val product: ProductEntity,
    val count: Int,
)

