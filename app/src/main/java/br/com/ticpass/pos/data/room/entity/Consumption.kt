package br.com.ticpass.pos.data.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import br.com.ticpass.pos.compose.utils.getCurrentDateString

@Entity(tableName = "consumptions" )
data class ConsumptionEntity(
    @PrimaryKey()
    val id: String = generateRandomEAN(),
    var accountable: String = "",
    var createdAt: String = getCurrentDateString(),
    var synced: Boolean = false,
    var event: String,
    var pos: String,
) {
    override fun toString() = id
}

data class _ConsumptionPopulated(
    @Embedded val consumption: ConsumptionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "consumption"
    )
    val resources: List<AcquisitionEntity>,
)

data class ConsumptionPopulated(
    private val consumption: ConsumptionEntity,
    val resources: List<AcquisitionEntity>,
) {
    val id: String
        get() = consumption.id

    val accountable: String
        get() = consumption.accountable

    val synced: Boolean
        get() = consumption.synced

    val createdAt: String
        get() = consumption.createdAt

    val amount: Long
        get() {
            return resources.sumOf{ it.price }
        }
}
