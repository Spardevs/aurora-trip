package br.com.ticpass.pos.data.room.repository


import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.OrderEntity
import br.com.ticpass.pos.data.room.entity.OrderPopulated
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val productDao: ProductDao,
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

    suspend fun getAllBySyncState(syncState: Boolean): List<OrderEntity> {
        return orderDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun setManySynced(orderIds: List<String>, syncState: Boolean) {
        if (orderIds.isEmpty()) return

        val orders = orderDao.getAllOrders() ?: emptyList()

        // Use forEach to update the 'synced' property
        orders.forEach { order ->
            if (orderIds.contains(order.id)) {
                order.synced = syncState
            }
        }

        // Update all modified orders in the database
        orderDao.updateMany(orders)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: OrderRepository? = null

        fun getInstance(orderDao: OrderDao, productDao: ProductDao) =
            instance ?: synchronized(this) {
                instance ?: OrderRepository(orderDao, productDao).also { instance = it }
            }
    }
}
