package br.com.ticpass.pos.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.ticpass.Constants.PASS_REPRINTING_MAX_RETRIES
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.compose.utils.generateRandomEAN

@Entity(tableName = "passes" )
data class PassEntity(
    @PrimaryKey val id: String = generateRandomEAN(),
    val createdAt: String = getCurrentDateString(),
    var accountable: String = "",

    @ColumnInfo(name = "printingRetries")
    var printingRetries: Int = 0,

    val order: String, // order id
    val event: String, // event id
    val pos: String, // pos id

    var isGrouped: Boolean = false,
    var synced: Boolean = false,
) {

    val isPrintingAllowed: Boolean
        get() {
            return printingRetries < PASS_REPRINTING_MAX_RETRIES
        }

    override fun toString() = id
}


data class _PassPopulated(
    @Embedded val pass: PassEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "pass"
    )
    val acquisitions: List<AcquisitionEntity>,
)

data class PassPopulated(
    val pass: PassEntity,
) {
    val id: String
        get() = pass.id

    val createdAt: String
        get() = pass.createdAt

    val accountable: String
        get() = pass.accountable

    val isPrintingAllowed = pass::isPrintingAllowed
}
