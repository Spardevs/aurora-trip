package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "acquisition",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class,  parentColumns = ["id"], childColumns = ["categoryId"], onDelete = CASCADE),
        ForeignKey(entity = ProductEntity::class,   parentColumns = ["id"], childColumns = ["productId"],  onDelete = CASCADE),
        ForeignKey(entity = OrderEntity::class,     parentColumns = ["id"], childColumns = ["orderId"],    onDelete = CASCADE),
        ForeignKey(entity = PassEntity::class,      parentColumns = ["id"], childColumns = ["passId"],     onDelete = CASCADE),
        ForeignKey(entity = MenuEntity::class,      parentColumns = ["id"], childColumns = ["menuId"],     onDelete = CASCADE),
        ForeignKey(entity = PosEntity::class,       parentColumns = ["id"], childColumns = ["posId"],      onDelete = CASCADE)
    ],
    indices = [
        Index("categoryId"),
        Index("productId"),
        Index("orderId"),
        Index("passId"),
        Index("menuId"),
        Index("posId")
    ]
)
data class AcquisitionEntity(
    @PrimaryKey val id: String,
    val createdAt: String,
    val name: String,
    val logo: String,
    val price: Long,
    val commission: Long,
    val categoryId: String,
    val productId: String,
    val orderId: String,
    val passId: String,
    val menuId: String,
    val posId: String,
    val synced: Boolean
)