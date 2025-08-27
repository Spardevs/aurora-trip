package br.com.ticpass.pos.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    var name: String,
    @ColumnInfo(defaultValue = "")
    val thumbnail: String,
    val url: String,
    @ColumnInfo(name = "categoryId")
    val categoryId: String,
    val price: Long,
    @ColumnInfo(defaultValue = "-1")
    val stock: Int,
    var isEnabled: Boolean = true
) : Parcelable {
    override fun toString(): String {
        return "Product(id=$id, title='$name')"
    }}

data class CategoryWithProducts(
    @Embedded
    val category: CategoryEntity,
    @Relation(
        parentColumn  = "id",
        entityColumn  = "categoryId"
    )
    val products: List<ProductEntity>
)

data class CategoryWithEnabledProducts(
    @Embedded
    val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val allProducts: List<ProductEntity>
) {
    val enabledProducts: List<ProductEntity>
        get() = allProducts.filter { it.isEnabled }
}

@Parcelize
data class CartItem(
    val product: ProductEntity,
    val quantity: Int,
    val observation: String? = null
) : Parcelable
