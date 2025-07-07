package br.com.ticpass.pos.data.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.entity.PaymentEntity

@Entity(tableName = "orders" )
data class OrderEntity(
    @PrimaryKey val id: String,
    var createdAt: String = getCurrentDateString(),
    var coords: String? = null,
    var synced: Boolean = false,
) {

    override fun toString() = id
}

data class AcquisitionOrderLine(
    var acquisition: AcquisitionEntity,
    var count: Int,
)

data class _OrderPopulated(
    @Embedded val order: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "order"
    )
    val acquisitions: List<AcquisitionEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "order"
    )
    val payments: List<PaymentEntity>,
)

data class OrderPopulated(
    val order: OrderEntity,
    private val  acquisitions: List<AcquisitionEntity>,
    val payments: List<PaymentEntity>
) {
    val id: String
        get() = order.id

    val amount: Long
        get() = payments.sumOf { it.amount }

    val date: String
        get() = order.createdAt

    val coords: String?
        get() = order.coords

    val orderlines: List<AcquisitionOrderLine>
        get() {

            val orderLineList: List<AcquisitionOrderLine> = acquisitions
                .groupingBy { it.product }
                .fold(0) { acc, _ -> acc + 1 }
                .map { (productId, count) ->

                    val acquisition = acquisitions.first { it.product == productId }

                    AcquisitionOrderLine(
                        acquisition = acquisition,
                        count = count,
                    )
                }

            return orderLineList
        }
}