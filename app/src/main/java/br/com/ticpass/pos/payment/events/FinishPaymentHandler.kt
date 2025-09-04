package br.com.ticpass.pos.payment.events

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import br.com.ticpass.pos.data.room.repository.OrderRepository
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.data.room.entity.OrderEntity
import java.util.UUID
import androidx.core.content.edit
import br.com.ticpass.pos.data.room.entity.PaymentEntity
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.EventRepository
import br.com.ticpass.pos.data.room.repository.PaymentRepository
import br.com.ticpass.pos.data.room.repository.PosRepository
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import jakarta.inject.Inject
import org.json.JSONObject
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.repository.AcquisitionRepository
import br.com.ticpass.pos.compose.utils.generateRandomEAN

@ActivityScoped
class FinishPaymentHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val eventRepository: EventRepository,
    private val posRepository: PosRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val acquisitionRepository: AcquisitionRepository // Adicionado repositório de aquisições
) {
    private val shoppingCartPrefs by lazy {
        appContext.getSharedPreferences("ShoppingCartPrefs", MODE_PRIVATE)
    }

    private suspend fun logCategoriesWithProducts() {
        try {
            val categoriesWithProducts = categoryRepository.getCategoriesWithProducts()

            categoriesWithProducts.forEach { categoryWithProducts ->
                Log.d("CategoryDebug", "Category: ${categoryWithProducts.category.name}")
                Log.d("CategoryDebug", "Products count: ${categoryWithProducts.products.size}")

                categoryWithProducts.products.forEach { product ->
                    Log.d("CategoryDebug", "  - Product: ${product.name}, ID: ${product.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("FinishPaymentHandler", "Error fetching categories with products: ${e.message}")
        }
    }

    suspend fun handlePayment(paymentType: PaymentType, paymentData: PaymentUIUtils.PaymentData? = null) {
        logCategoriesWithProducts()

        val productQuantities = getProductsFromShoppingCart()

        if (productQuantities.isEmpty()) {
            Log.e("FinishPaymentHandler", "Shopping cart is empty! Cannot process payment.")
            return
        }

        when (paymentType) {
            PaymentType.SINGLE_PAYMENT -> handleSinglePayment(paymentData, productQuantities)
            PaymentType.MULTI_PAYMENT -> handleMultiPayment(paymentData, productQuantities)
        }
    }

    private suspend fun getProductsFromShoppingCart(): Map<String, Int> {
        val productQuantities = mutableMapOf<String, Int>()

        try {
            val shoppingCartJson = shoppingCartPrefs.getString("shopping_cart_data", null)

            // DEBUG: Log the raw JSON to see the actual structure
            Log.d("FinishPaymentHandler", "Raw shopping cart JSON: $shoppingCartJson")

            if (shoppingCartJson != null) {
                val jsonObject = JSONObject(shoppingCartJson)

                // DEBUG: Log all keys in the JSON object
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    Log.d("FinishPaymentHandler", "JSON key: $key, value: ${jsonObject.get(key)}")
                }

                // Check if "items" exists and is an object
                if (jsonObject.has("items")) {
                    val itemsObject = jsonObject.getJSONObject("items")
                    val productIds = itemsObject.keys().asSequence().toList()

                    Log.d("FinishPaymentHandler", "Product IDs found in shopping cart: $productIds")

                    for (productId in productIds) {
                        try {
                            val quantity = itemsObject.getInt(productId)
                            val product = productRepository.getById(productId)

                            if (product != null) {
                                productQuantities[productId] = quantity
                                Log.d("FinishPaymentHandler", "Product: ${product.name} (ID: $productId), Quantity: $quantity")
                            } else {
                                Log.e("FinishPaymentHandler", "Product not found: $productId")
                            }
                        } catch (e: Exception) {
                            Log.e("FinishPaymentHandler", "Error fetching product $productId: ${e.message}")
                        }
                    }
                } else {
                    Log.d("FinishPaymentHandler", "No 'items' object found in shopping cart data")
                }
            } else {
                Log.d("FinishPaymentHandler", "No shopping cart data found")
            }
        } catch (e: Exception) {
            Log.e("FinishPaymentHandler", "Error parsing shopping cart data: ${e.message}")
        }

        return productQuantities
    }

    private suspend fun handleSinglePayment(
        paymentData: PaymentUIUtils.PaymentData?,
        productQuantities: Map<String, Int>
    ) {
        val orderId = UUID.randomUUID().toString()
        val order = OrderEntity(
            id = orderId,
            createdAt = getCurrentDateString(),
            coords = null,
            synced = false
        )

        try {
            orderRepository.insertOrder(order)
            paymentData?.let {
                createPayment(orderId, it)
            }
            createAcquisitions(orderId, productQuantities)

            println("Created single payment order: $orderId with ${productQuantities.size} acquisitions")
        } catch (e: Exception) {
            println("Failed to create single payment order: ${e.message}")
        }
    }

    private suspend fun handleMultiPayment(
        paymentData: PaymentUIUtils.PaymentData?,
        productQuantities: Map<String, Int>
    ) {
        val existingOrderId = shoppingCartPrefs.getString("multi_payment_order_id", null)

        if (existingOrderId == null) {
            createNewMultiPaymentOrder(paymentData, productQuantities)
        } else {
            verifyAndMaintainExistingOrder(existingOrderId, paymentData, productQuantities)
        }
    }

    private suspend fun createNewMultiPaymentOrder(
        paymentData: PaymentUIUtils.PaymentData?,
        productQuantities: Map<String, Int>
    ) {
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
            paymentData?.let {
                createPayment(newOrderId, it)
            }

            createAcquisitions(newOrderId, productQuantities)

            println("Created new multi-payment order: $newOrderId with ${productQuantities.size} acquisitions")
        } catch (e: Exception) {
            shoppingCartPrefs.edit {
                remove("multi_payment_order_id")
            }
            println("Failed to create multi-payment order: ${e.message}")
        }
    }

    private suspend fun verifyAndMaintainExistingOrder(
        orderId: String,
        paymentData: PaymentUIUtils.PaymentData?,
        productQuantities: Map<String, Int>
    ) {
        val orderExists = orderRepository.getOrderById(orderId) != null
        if (orderExists) {
            paymentData?.let {
                createPayment(orderId, it)
            }

            createAcquisitions(orderId, productQuantities)

            println("Added payment and ${productQuantities.size} acquisitions to existing multi-payment order: $orderId")
        } else {
            shoppingCartPrefs.edit {
                remove("multi_payment_order_id")
            }
            createNewMultiPaymentOrder(paymentData, productQuantities)
        }
    }

    private suspend fun createAcquisitions(orderId: String, productQuantities: Map<String, Int>) {
        try {
            val eventData = eventRepository.getFirstEvent()
            val posData = posRepository.getFirstPos()

            if (eventData == null || posData == null) {
                Log.e("FinishPaymentHandler", "Event or POS data not found")
                return
            }

            val categoriesWithProducts = categoryRepository.getCategoriesWithProducts()
            val productCategoryMap = mutableMapOf<String, String>()

            categoriesWithProducts.forEach { categoryWithProducts ->
                categoryWithProducts.products.forEach { product ->
                    productCategoryMap[product.id] = categoryWithProducts.category.name
                }
            }

            val acquisitions = mutableListOf<AcquisitionEntity>()

            for ((productId, quantity) in productQuantities) {
                val product = productRepository.getById(productId)

                if (product != null) {
                    val categoryName = productCategoryMap[productId] ?: "Unknown Category"

                    for (i in 1..quantity) {
                        val acquisition = AcquisitionEntity(
                            id = generateRandomEAN(),
                            name = product.name,
                            logo = product.thumbnail,
                            price = product.price,
                            commission = posData.commission,
                            category = categoryName,
                            product = productId,
                            order = orderId,
                            pass = "",
                            event = eventData.id,
                            pos = posData.id,
                            synced = false
                        )
                        acquisitions.add(acquisition)
                    }
                } else {
                    Log.e("FinishPaymentHandler", "Product not found for ID: $productId")
                }
            }

            if (acquisitions.isNotEmpty()) {
                acquisitionRepository.insertMany(acquisitions)
                Log.d("FinishPaymentHandler", "Created ${acquisitions} acquisitions for order: $orderId")
            }

        } catch (e: Exception) {
            Log.e("FinishPaymentHandler", "Error creating acquisitions: ${e.message}")
        }
    }

    private suspend fun createPayment(orderId: String, paymentData: PaymentUIUtils.PaymentData) {
        Log.d("FinishPaymentHandler", "Creating payment for order: $orderId")

        val posData = posRepository.getFirstPos()

        val payment = PaymentEntity(
            id = UUID.randomUUID().toString(),
            type = paymentData.method.toString(),
            amount = paymentData.amount.toLong(),
            commission = posData?.commission ?: 0L,
            createdAt = System.currentTimeMillis().toString(),
            order = orderId,
            synced = false
        )

        paymentRepository.insertPayment(payment)
    }
}

enum class PaymentType {
    SINGLE_PAYMENT,
    MULTI_PAYMENT
}