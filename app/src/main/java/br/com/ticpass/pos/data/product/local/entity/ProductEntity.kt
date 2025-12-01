package br.com.ticpass.pos.data.product.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "thumbnail") val thumbnail: String,
    @ColumnInfo(name = "price") val price: Long,
    @ColumnInfo(name = "stock") val stock: Int,
    @ColumnInfo(name = "isEnabled") val isEnabled: Boolean
)