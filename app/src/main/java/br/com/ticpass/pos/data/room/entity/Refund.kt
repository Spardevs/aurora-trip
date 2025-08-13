package br.com.ticpass.pos.data.room.entity

import android.util.Log
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.dataStore
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "refunds")
data class RefundEntity(
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
        createdAt = getCurrentDateString(),
        event = event,
        pos = pos
    ) {
        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(handler) {
            if (accountable.isNullOrEmpty()) {
                this@RefundEntity.accountable = getCashierName()
                return@launch
            }

            this@RefundEntity.accountable = accountable
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

data class _RefundPopulated(
    @Embedded val refund: RefundEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "refund"
    )
    val exchanges: List<AcquisitionEntity>,
)

data class RefundPopulated(
    private val refund: RefundEntity,
    val resources: List<AcquisitionEntity>,
) {
    val id: String
        get() = refund.id

    val accountable: String
        get() = refund.accountable

    val synced: Boolean
        get() = refund.synced

    val createdAt: String
        get() = refund.createdAt

    val amount: Long
        get() {
            return resources.sumOf { it.price }
        }
}
