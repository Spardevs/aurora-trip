package br.com.ticpass.pos.data.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import br.com.ticpass.pos.compose.utils.generateObjectId

@Entity(tableName = "voucherExchangeProducts" )
data class VoucherExchangeProductEntity(
    @PrimaryKey val id: String = generateRandomEAN(),
    val createdAt: String = getCurrentDateString(),
    var name: String,
    var productId: String,
    val thumbnail: String,
    val url: String,
    val category: String,
    val price: Long,
    var count: Int,
    val voucher: String,
)

data class _VoucherExchangeProductPopulated(
    @Embedded val exchangeProduct: VoucherExchangeProductEntity,
)

data class VoucherExchangeProductPopulated(
    private val exchangeProduct: VoucherExchangeProductEntity,
) {
    val id: String
        get() = exchangeProduct.id

    val productId: String
        get() = exchangeProduct.productId

    val voucher: String
        get() = exchangeProduct.voucher

    val name: String
        get() = exchangeProduct.name

    val thumbnail: String
        get() = exchangeProduct.thumbnail

    val url: String
        get() = exchangeProduct.url

    val category: String
        get() = exchangeProduct.category

    val price: Long
        get() = exchangeProduct.price

    val count: Int
        get() = exchangeProduct.count

    val createdAt: String
        get() = exchangeProduct.createdAt
}
