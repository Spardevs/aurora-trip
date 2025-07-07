package br.com.ticpass.pos.data.room.repository

import android.util.Log
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.data.room.dao.CartOrderLineDao
import br.com.ticpass.pos.data.room.entity.CartOrderLineEntity
import br.com.ticpass.pos.data.room.entity._CartOrderLineEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartOrderLineRepository @Inject constructor(
    private val cartOrderLineDao: CartOrderLineDao,
    private val productDao: ProductDao
) {
    suspend fun getAll(): List<CartOrderLineEntity> {

        val cartOrderLines = cartOrderLineDao.getAll()
        val productIds = cartOrderLines.map { it.product }
        val products = productDao.getByIds(productIds)

        val populated = cartOrderLines
            .filter { orderLine ->
                val product = products.find { product ->
                    product.id == orderLine.product
                }

                product !== null
            }
            .map { orderLine ->
                val product = products.find { product ->
                    product.id == orderLine.product
                }

                CartOrderLineEntity(
                    count = orderLine.count,
                    product = product as ProductEntity
                )
            }

        return populated
    }

    suspend fun insertMany(cartOrderLines: List<_CartOrderLineEntity>): List<CartOrderLineEntity> {
        val productIds = cartOrderLines.map { it.product }
        val all = cartOrderLineDao.getManyByProductId(productIds)
        val unsetProducts: List<String> = productIds.filter { productId -> all.none { it.product == productId } }
        val unsetOrderLines = cartOrderLines.filter { unsetProducts.contains(it.product) }
        cartOrderLineDao.insertMany(unsetOrderLines)
        return this.getAll()
    }

    suspend fun increaseOrderLineCount(productId: String): List<CartOrderLineEntity> {
        val orderLine = cartOrderLineDao.getByProductId(productId)
        Log.d("orderLine", orderLine.toString())
        if (orderLine != null) {
            orderLine.count += 1
            cartOrderLineDao.updateCartOrderLine(orderLine)
        } else {
            cartOrderLineDao.insertMany(listOf(_CartOrderLineEntity(productId, 1)))
        }
        return this.getAll()
    }

    suspend fun decreaseOrderLineCount(productId: String): List<CartOrderLineEntity> {
        val orderLine = cartOrderLineDao.getByProductId(productId)
        if (orderLine != null) {
            orderLine.count -= 1
            if (orderLine.count == 0) {
                cartOrderLineDao.deleteByProductId(productId)
            } else {
                cartOrderLineDao.updateCartOrderLine(orderLine)
            }
        }
        return this.getAll()
    }

    suspend fun clearAll(): List<CartOrderLineEntity> {
        cartOrderLineDao.clearAll()
        return this.getAll()
    }

    suspend fun deleteByProductId(productId: String): List<CartOrderLineEntity> {
        cartOrderLineDao.deleteByProductId(productId)
        return this.getAll()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: CartOrderLineRepository? = null

        fun getInstance(cartOrderLineDao: CartOrderLineDao, productDao: ProductDao) =
            instance ?: synchronized(this) {
                instance ?: CartOrderLineRepository(cartOrderLineDao, productDao).also { instance = it }
            }
    }
}
