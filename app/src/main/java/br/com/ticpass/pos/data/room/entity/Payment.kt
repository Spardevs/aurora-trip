package br.com.ticpass.pos.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.compose.utils.generateRandomEAN

@Entity(tableName = "payments" )
data class PaymentEntity(
    @PrimaryKey val id: String = generateRandomEAN(),
    var acquirerTransactionKey: String = "",
    var amount: Long,
    @ColumnInfo(defaultValue = "0")
    var commission: Long = 0L,
    var createdAt: String = getCurrentDateString(),
    var order: String,
    var type: String,
    var usedAcquirer: Boolean = true,
    var synced: Boolean = false,
    var transactionId: String = "",
) {
    override fun toString() = id
}
