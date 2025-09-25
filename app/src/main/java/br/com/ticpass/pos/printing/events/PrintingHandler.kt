package br.com.ticpass.pos.printing.events

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.view.ui.pass.PassScreen
import br.com.ticpass.pos.view.ui.pass.PassType
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import br.com.ticpass.pos.util.savePassAsBitmap
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.iterator

class ProductRepository(private val productDao: ProductDao, private val posDao: PosDao, private val eventDao: EventDao) {
    suspend fun getProductById(id: String) = productDao.getById(id)
    suspend fun getAllPos() = posDao.getAll()
    suspend fun getAllEvents() = eventDao.getAllEvents()
}

class PrintingHandler(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val repository by lazy {
        ProductRepository(
            AppDatabase.getInstance(context).productDao(),
            AppDatabase.getInstance(context).posDao(),
            AppDatabase.getInstance(context).eventDao()
        )
    }

    // alterar assinatura para receber o ViewModel
    fun generateTickets(
        composeView: ComposeView,
        passType: PassType,
        printingViewModel: PrintingViewModel
    ) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                val operatorName = getOperatorName()
                val (products, pos, event) = loadDataFromDatabase()
                val passList = buildPassList(products, pos, event, operatorName, passType)

                composeView.setContent {
                    PassScreenContainer(passType = passType, passList = passList)
                }

                passList.forEach { passData ->
                    val file = savePassAsBitmap(context, passType, passData)
                    if (file == null) {
                        Log.e("PrintingHandler", "Falha ao salvar pass como bitmap para product ${passData.productData?.name}")
                    } else {
                        Log.d("PrintingHandler", "Pass salvo em: ${file.absolutePath}")
                        printingViewModel.enqueuePrinting(file.absolutePath, PrintingProcessorType.MP_4200_HS)
                    }
                }

                // se seu queue precisa ser iniciado manualmente, chame:
                // printingViewModel.startProcessing()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    @Composable
    private fun PassScreenContainer(
        passType: PassType,
        passList: List<PassData>
    ) {
        PassScreen(
            passType = passType,
            passList = passList,
            modifier = Modifier.fillMaxWidth()
        )
    }

    private fun getCartItems(): Map<String, Int> {
        val prefs = context.getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("shopping_cart_data", null) ?: return emptyMap()

        val jsonObject = JSONObject(json)
        val items = jsonObject.getJSONObject("items")
        val map = mutableMapOf<String, Int>()

        items.keys().forEach { key ->
            map[key] = items.getInt(key)
        }

        return map
    }

    private fun getOperatorName(): String {
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("operator_name", "Atendente") ?: "Atendente"
    }

    private suspend fun loadDataFromDatabase(): Triple<List<ProductEntity>, PosEntity?, EventEntity?> {
        val db = AppDatabase.getInstance(context)
        val cartItems = getCartItems()
        val products = mutableListOf<ProductEntity>()

        for ((id, _) in cartItems) {
            db.productDao().getById(id)?.let { products.add(it) }
        }

        val pos = db.posDao().getAll().firstOrNull { it.isSelected }
        val event = db.eventDao().getAllEvents().firstOrNull { it.isSelected }

        return Triple(products, pos, event)
    }

    private fun buildPassList(
        products: List<ProductEntity>,
        pos: PosEntity?,
        event: EventEntity?,
        operatorName: String,
        passType: PassType
    ): List<PassData> {
        val cartItems = getCartItems()

        return if (products.isNotEmpty()) {
            when (passType) {
                PassType.ProductCompact, PassType.ProductExpanded -> {
                    products.flatMap { product ->
                        val quantity = cartItems[product.id] ?: 1
                        // Cria uma entrada separada para cada unidade do produto
                        (1..quantity).map { unitIndex ->
                            PassData(
                                header = PassData.HeaderData(
                                    title = event?.name ?: "ticpass",
                                    date = event?.getFormattedStartDate() ?: "",
                                    barcode = "0000000002879" // pode ser dinâmico depois
                                ),
                                productData = PassData.ProductData(
                                    name = "${product.name} (${unitIndex}/$quantity)",
                                    price = "R$ ${(product.price / 100.0).format(2)}",
                                    eventTitle = event?.name ?: "",
                                    eventTime = event?.getFormattedStartDate() ?: ""
                                ),
                                footer = PassData.FooterData(
                                    cashierName = operatorName,
                                    menuName = pos?.name ?: "POS",
                                    description = "Ficha válida por 15 dias após a emissão...",
                                    printTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
                                ),
                                showCutLine = true
                            )
                        }
                    }
                }

                PassType.ProductGrouped -> {
                    val groupedItems = products.map { product ->
                        val quantity = cartItems[product.id] ?: 1
                        PassData.GroupedData.GroupedItem(
                            quantity = quantity,
                            name = product.name,
                            price = "R$ ${(product.price / 100.0 * quantity).format(2)}"
                        )
                    }

                    val totalItems = groupedItems.sumOf { it.quantity }
                    val totalPrice = groupedItems.sumOf {
                        it.price.replace("R$", "").replace(",", ".").trim().toDouble()
                    }

                    listOf(
                        PassData(
                            header = PassData.HeaderData(
                                title = event?.name ?: "ticpass",
                                date = event?.getFormattedStartDate() ?: "",
                                barcode = "0000000002879"
                            ),
                            groupedData = PassData.GroupedData(
                                items = groupedItems,
                                totalItems = totalItems,
                                totalPrice = "R$ ${totalPrice.format(2)}"
                            ),
                            footer = PassData.FooterData(
                                cashierName = operatorName,
                                menuName = pos?.name ?: "POS",
                                description = "Ficha válida por 15 dias após a emissão...",
                                printTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
                            )
                        )
                    )
                }
            }
        } else {
            emptyList()
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}