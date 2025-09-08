package br.com.ticpass.pos.data.room.service

import android.content.Context
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.util.savePassAsBitmap
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.view.ui.pass.PassType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PassGeneratorService @Inject constructor(
    private val context: Context
) {

    private val productDao: ProductDao by lazy {
        AppDatabase.getInstance(context).productDao()
    }

    private val posDao: PosDao by lazy {
        AppDatabase.getInstance(context).posDao()
    }

    private val eventDao: EventDao by lazy {
        AppDatabase.getInstance(context).eventDao()
    }

    suspend fun generateAndSavePasses(passType: PassType = PassType.ProductGrouped): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val operatorName = getOperatorName()
                val (products, pos, event) = loadDataFromDatabase()
                val passList = buildPassList(products, pos, event, operatorName, passType)

                // Salva cada pass como bitmap e retorna os caminhos dos arquivos
                passList.mapNotNull { passData ->
                    savePassAsBitmap(context, passType, passData)?.absolutePath
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun printPassesAutomatically(passType: PassType = PassType.ProductGrouped) {
        withContext(Dispatchers.IO) {
            val filePaths = generateAndSavePasses(passType)

            // Aqui você implementaria a lógica de impressão
            filePaths.forEach { filePath ->
                printPassFile(filePath)
            }

            // Opcional: limpar os arquivos após impressão
            cleanUpGeneratedFiles(filePaths)
        }
    }

    private fun printPassFile(filePath: String) {
        // Implementar lógica de impressão aqui
        // Isso dependerá da sua biblioteca/API de impressão
        println("Imprimindo arquivo: $filePath")

        // Exemplo com impressora térmica:
        // thermalPrinter.printBitmap(File(filePath))
    }

    private fun cleanUpGeneratedFiles(filePaths: List<String>) {
        filePaths.forEach { path ->
            File(path).delete()
        }
    }

    private fun getOperatorName(): String {
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("operator_name", "Atendente") ?: "Atendente"
    }

    private suspend fun loadDataFromDatabase(): Triple<List<ProductEntity>, PosEntity?, EventEntity?> {
        val cartItems = getCartItems()
        val products = mutableListOf<ProductEntity>()

        for ((id, _) in cartItems) {
            productDao.getById(id)?.let { products.add(it) }
        }

        val pos = posDao.getAll().firstOrNull { it.isSelected }
        val event = eventDao.getAllEvents().firstOrNull { it.isSelected }

        return Triple(products, pos, event)
    }

    private fun getCartItems(): Map<String, Int> {
        val prefs = context.getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("shopping_cart_data", null) ?: return emptyMap()

        return try {
            val jsonObject = JSONObject(json)
            val items = jsonObject.getJSONObject("items")
            val map = mutableMapOf<String, Int>()

            items.keys().forEach { key ->
                map[key] = items.getInt(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getCartObservations(): Map<String, String> {
        val prefs = context.getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("shopping_cart_data", null) ?: return emptyMap()

        return try {
            val jsonObject = JSONObject(json)
            val observations = jsonObject.optJSONObject("observations") ?: return emptyMap()
            val map = mutableMapOf<String, String>()

            observations.keys().forEach { key ->
                observations.getString(key).let { value ->
                    map[key] = value
                }
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun buildPassList(
        products: List<ProductEntity>,
        pos: PosEntity?,
        event: EventEntity?,
        operatorName: String,
        passType: PassType
    ): List<PassData> {
        val cartItems = getCartItems()
        val cartObservations = getCartObservations()

        return if (products.isNotEmpty()) {
            when (passType) {
                PassType.ProductCompact, PassType.ProductExpanded -> {
                    products.flatMap { product ->
                        val quantity = cartItems[product.id] ?: 1
                        val observation = cartObservations[product.id]
                        (1..quantity).map { unitIndex ->
                            PassData(
                                header = PassData.HeaderData(
                                    title = event?.name ?: "ticpass",
                                    date = event?.getFormattedStartDate() ?: "",
                                    barcode = "0000000002879"
                                ),
                                productData = PassData.ProductData(
                                    name = "${product.name} (${unitIndex}/$quantity)",
                                    price = "R$ ${(product.price / 100.0).format(2)}",
                                    eventTitle = event?.name ?: "",
                                    eventTime = event?.getFormattedStartDate() ?: "",
                                    observation = observation
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
                        val observation = cartObservations[product.id]

                        PassData.GroupedData.GroupedItem(
                            quantity = quantity,
                            name = product.name,
                            price = "R$ ${(product.price / 100.0 * quantity).format(2)}",
                            observation = observation
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