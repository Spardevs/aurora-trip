package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.model.History
import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.entity.ProductEntity
import java.text.SimpleDateFormat
import java.util.*

class HistoryRepository(
    private val orderDao: OrderDao
) {
    suspend fun getHistories(): List<History> {
        val orders = orderDao.getAllPopulated()
        return orders.map { populated ->
            val payments = populated.payments
            val acquisitions = populated.acquisitions

            // Agrupa acquisitions por produto para contar quantidades
            val productQuantities = acquisitions.groupingBy { it.product }.eachCount()

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")



            History(
                id = populated.order.id,
                transactionId = payments.firstOrNull()?.transactionId.toString(),
                atk = payments.firstOrNull()?.acquirerTransactionKey.toString(),
                totalPrice = payments.sumOf { it.amount } / 1000.0,
                paymentPrice = payments.sumOf { it.amount } / 1000.0 - payments.sumOf { it.commission } / 1000.0,
                commissionPrice = payments.sumOf { it.commission } / 1000.0,
                date = try {
                    isoFormat.parse(populated.order.createdAt) ?: Date()
                } catch (e: Exception) {
                    Date()
                },
                paymentMethod = payments.firstOrNull()?.type ?: "Desconhecido",
                description = "Venda concluída",
                products = acquisitions.distinctBy { it.product }.map { acq ->
                    Pair(
                        ProductEntity(
                            id = acq.product,
                            name = acq.name,
                            thumbnail = acq.logo,
                            url = "",
                            categoryId = acq.category,
                            price = acq.price,
                            stock = 0,
                            isEnabled = true
                        ),
                        productQuantities[acq.product] ?: 1
                    )
                }
            )
        }
    }
}