package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.entity.OrderEntity
import br.com.ticpass.pos.data.room.entity.OrderPopulated
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao
) {

    suspend fun insertMany(orders: List<OrderEntity>) {
        return orderDao.insertOrders(orders)
    }

    suspend fun getById(orderId: String): OrderEntity? {
        return orderDao.getById(orderId)
    }

    suspend fun deleteOld(keep: Int = 30): Unit {
        return orderDao.deleteOld(keep)
    }

    suspend fun getAllPopulated(): List<OrderPopulated> {
        val ordersPopulated = orderDao.getAllPopulated()

        val mappedList = ordersPopulated.map { item ->
            OrderPopulated(
                order = item.order,
                payments = item.payments,
                acquisitions = item.acquisitions
            )
        }

        return mappedList
    }

    suspend fun getAll() = orderDao.getAllOrders()
    fun getLast() = orderDao.getLast()

    suspend fun insertOrder(order: OrderEntity) {
        return orderDao.insertOrders(listOf(order))
    }

    suspend fun getOrderById(orderId: String): OrderEntity? {
        return orderDao.getById(orderId)
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<OrderEntity> {
        return orderDao.getBySyncState(syncState)
    }

    suspend fun setManySynced(orderIds: List<String>, syncState: Boolean) {
        if (orderIds.isEmpty()) return

        val orders = orderDao.getAllOrders()

        orders.forEach { order ->
            if (orderIds.contains(order.id)) {
                order.synced = syncState
            }
        }

        orderDao.updateMany(orders)
    }

    suspend fun insertSingle(order: OrderEntity) {
        return orderDao.insertOrders(listOf(order))
    }

}