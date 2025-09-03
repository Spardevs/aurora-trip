package br.com.ticpass.pos.payment.events

import android.content.Context
import android.content.Context.MODE_PRIVATE
import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.repository.OrderRepository
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.data.room.entity.OrderEntity
import java.util.UUID
import androidx.core.content.edit
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import jakarta.inject.Inject
import jakarta.inject.Singleton

enum class PaymentType {
    SINGLE_PAYMENT,
    MULTI_PAYMENT
}

@ActivityScoped
class FinishPaymentHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val orderRepository: OrderRepository
) {
    private val shoppingCartPrefs by lazy {
        appContext.getSharedPreferences("ShoppingCartPrefs", MODE_PRIVATE)
    }

    suspend fun handlePayment(paymentType: PaymentType, paymentData: PaymentUIUtils.PaymentData? = null) {
        when (paymentType) {
            PaymentType.SINGLE_PAYMENT -> handleSinglePayment(paymentData)
            PaymentType.MULTI_PAYMENT -> handleMultiPayment(paymentData)
        }
    }

    private suspend fun handleSinglePayment(paymentData: PaymentUIUtils.PaymentData?) {
        val orderId = UUID.randomUUID().toString()
        val order = OrderEntity(
            id = orderId,
            createdAt = getCurrentDateString(),
            coords = null,
            synced = false
        )

        try {
            orderRepository.insertOrder(order)
            println("Created single payment order: $orderId")
        } catch (e: Exception) {
            println("Failed to create single payment order: ${e.message}")
        }
    }

    private suspend fun handleMultiPayment(paymentData: PaymentUIUtils.PaymentData?) {
        val existingOrderId = shoppingCartPrefs.getString("multi_payment_order_id", null)

        if (existingOrderId == null) {
            createNewMultiPaymentOrder()
        } else {
            verifyAndMaintainExistingOrder(existingOrderId, paymentData)
        }
    }

    private suspend fun createNewMultiPaymentOrder() {
        val newOrderId = UUID.randomUUID().toString()
        val newOrder = OrderEntity(
            id = newOrderId,
            createdAt = getCurrentDateString(),
            coords = null,
            synced = false
        )

        shoppingCartPrefs.edit {
            putString("multi_payment_order_id", newOrderId)
        }

        try {
            orderRepository.insertOrder(newOrder)
            println("Created new multi-payment order: $newOrderId")
        } catch (e: Exception) {
            shoppingCartPrefs.edit {
                remove("multi_payment_order_id")
            }
            println("Failed to create multi-payment order: ${e.message}")
        }
    }


    private suspend fun verifyAndMaintainExistingOrder(orderId: String, paymentData: PaymentUIUtils.PaymentData?) {
        val orderExists = orderRepository.getOrderById(orderId) != null

        if (orderExists) {
            addPaymentToOrder(orderId, paymentData)
            println("Added payment to existing multi-payment order: $orderId")
        } else {
            shoppingCartPrefs.edit {
                remove("multi_payment_order_id")
            }
            createNewMultiPaymentOrder()
        }
    }

    private suspend fun addPaymentToOrder(orderId: String, paymentData: PaymentUIUtils.PaymentData?) {
        // Implemente a lógica para adicionar pagamento à order
        // paymentRepository.insertPayment(PaymentEntity(orderId = orderId, ...))
    }

    fun clearMultiPaymentSession() {
        shoppingCartPrefs.edit {
            remove("multi_payment_order_id")
        }
    }
}