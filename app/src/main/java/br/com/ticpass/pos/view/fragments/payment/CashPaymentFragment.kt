package br.com.ticpass.pos.view.fragments.payment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
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
import br.com.ticpass.pos.util.savePassAsBitmap
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProductRepository(private val productDao: ProductDao, private val posDao: PosDao, private val eventDao: EventDao) {
    suspend fun getProductById(id: String) = productDao.getById(id)
    suspend fun getAllPos() = posDao.getAll()
    suspend fun getAllEvents() = eventDao.getAllEvents()
}


class CashPaymentFragment : Fragment() {

    private lateinit var btnCompact: Button
    private lateinit var btnExpanded: Button
    private lateinit var btnGrouped: Button
    private lateinit var composeView: ComposeView
    private lateinit var progressBar: ProgressBar
    private lateinit var rootView: View
    private lateinit var loadingContainer: View
    private lateinit var contentContainer: View
    private var selectedPassType: PassType = PassType.ProductGrouped

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_payment_cash, container, false)
        loadingContainer = rootView.findViewById(R.id.loadingContainer) // Adicione no seu XML
        contentContainer = rootView.findViewById(R.id.contentContainer) // Adicione no seu XML
        return rootView
    }

    private val repository by lazy {
        ProductRepository(
            AppDatabase.getInstance(requireContext()).productDao(),
            AppDatabase.getInstance(requireContext()).posDao(),
            AppDatabase.getInstance(requireContext()).eventDao()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCompact = view.findViewById(R.id.btnCompact)
        btnExpanded = view.findViewById(R.id.btnExpanded)
        btnGrouped = view.findViewById(R.id.btnGrouped)
        composeView = view.findViewById(R.id.passComposeView)
        loadingContainer = view.findViewById(R.id.loadingContainer)
        contentContainer = view.findViewById(R.id.contentContainer)

        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        btnCompact.setOnClickListener { changePassType(PassType.ProductCompact) }
        btnExpanded.setOnClickListener { changePassType(PassType.ProductExpanded) }
        btnGrouped.setOnClickListener { changePassType(PassType.ProductGrouped) }

        loadAndShowPasses(selectedPassType)
    }

    private fun changePassType(newType: PassType) {
        if (selectedPassType == newType) return
        println("Changing pass type from $selectedPassType to $newType")
        selectedPassType = newType
        loadAndShowPasses(selectedPassType)
    }

    private fun loadAndShowPasses(passType: PassType) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val operatorName = getOperatorName()
                val (products, pos, event) = loadDataFromDatabase()
                val passList = buildPassList(products, pos, event, operatorName, passType)

                // Atualiza a UI primeiro
                composeView.setContent {
                    PassScreenContainer(passType = passType, passList = passList)
                }

                // Depois gera os bitmaps
                passList.forEach { passData ->
                    savePassAsBitmap(requireContext(), passType, passData)
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
        contentContainer.alpha = if (show) 0.5f else 1f

        btnCompact.isEnabled = !show
        btnExpanded.isEnabled = !show
        btnGrouped.isEnabled = !show
    }

    fun View.doOnNextLayout(action: (View) -> Unit) {
        addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                removeOnLayoutChangeListener(this)
                action(v)
            }
        })
    }


    private fun getCartObservations(): Map<String, String> {
        val prefs = requireContext().getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
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


    private suspend fun renderPassScreen(passType: PassType, composeView: ComposeView) {
        val operatorName = getOperatorName()
        val (products, pos, event) = loadDataFromDatabase()
        val passList = buildPassList(products, pos, event, operatorName, passType)

        composeView.setContent {
            PassScreen(passType = selectedPassType, passList = passList)
        }

    }

    private fun getCartItems(): Map<String, Int> {
        val prefs = requireContext().getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
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
        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("operator_name", "Atendente") ?: "Atendente"
    }

    private suspend fun loadDataFromDatabase(): Triple<List<ProductEntity>, PosEntity?, EventEntity?> {
        val db = AppDatabase.getInstance(requireContext())
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