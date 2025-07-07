package br.com.ticpass.pos.data.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import br.com.ticpass.pos.compose.utils.generateObjectId

@Entity(tableName = "voucherRedemptions" )
data class VoucherRedemptionEntity(
    @PrimaryKey val id: String = generateRandomEAN(),
    val createdAt: String = getCurrentDateString(),
    val amount: Long = 0L,
    val voucher: String,
    val order: String,
    var synced: Boolean = false,
)

data class _VoucherRedemptionPopulated(
    @Embedded val redemption: VoucherRedemptionEntity,
)

data class VoucherRedemptionPopulated(
    private val redemption: VoucherRedemptionEntity,
) {
    val id: String
        get() = redemption.id

    val voucher: String
        get() = redemption.voucher

    val order: String
        get() = redemption.order

    val amount: Long
        get() = redemption.amount

    val createdAt: String
        get() = redemption.createdAt
}
