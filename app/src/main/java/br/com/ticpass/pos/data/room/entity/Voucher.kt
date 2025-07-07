package br.com.ticpass.pos.data.room.entity

import android.util.Log
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionEntity
import br.com.ticpass.pos.dataStore
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey() val id: String = generateRandomEAN(),
    var accountable: String = "",
    var createdAt: String = getCurrentDateString(),
    var synced: Boolean = false,
    var event: String,
    var pos: String,
) {

    constructor(
        accountable: String?,
        event: String,
        pos: String,
    ) : this(
        accountable = "",
        event = event,
        pos = pos
    ) {

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(handler) {
            if (accountable.isNullOrEmpty()) {
                this@VoucherEntity.accountable = getCashierName()
                return@launch
            }

            this@VoucherEntity.accountable = accountable
        }
    }

    private suspend fun getCashierName(): String {
        return try {
            val authManager = AuthManager(MainActivity.appContext.dataStore)
            authManager.getCashierName()
        } catch (e: Exception) {
            Log.d("getCashierName:exception", e.toString())
            ""
        }
    }

    override fun toString() = id
}


data class _VoucherPopulated(
    @Embedded val voucher: VoucherEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "voucher"
    )
    val exchanges: List<AcquisitionEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "voucher"
    )
    val redemptions: List<VoucherRedemptionEntity>,
)

data class VoucherPopulated(
    private val voucher: VoucherEntity,
    val resources: List<AcquisitionEntity>,
    val redemptions: List<VoucherRedemptionEntity>,
) {
    val id: String
        get() = voucher.id

    val accountable: String
        get() = voucher.accountable

    val synced: Boolean
        get() = voucher.synced

    val initial: Long
        get() {
            return resources.sumOf { it.price }
        }

    val balance: Long
        get() {
            val redemptionsAmount = redemptions.sumOf { it.amount }
            return initial - redemptionsAmount
        }

    val createdAt: String
        get() = voucher.createdAt
}
