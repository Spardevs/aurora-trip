package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val name: String,
    val thumbnail: String,
    val url: String,
    val price: Long,
    val stock: Int,
    val isEnabled: Boolean
)