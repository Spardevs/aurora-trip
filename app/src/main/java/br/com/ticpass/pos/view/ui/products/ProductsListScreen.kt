package br.com.ticpass.pos.view.ui.products

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.SplitEqualActivity
import br.com.ticpass.pos.data.activity.SplitManualActivity
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.view.ui.payment.PaymentScreen
import br.com.ticpass.pos.view.ui.products.adapter.CategoriesPagerAdapter
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartScreen
import br.com.ticpass.pos.viewmodel.payment.PaymentMethod
import br.com.ticpass.pos.viewmodel.products.ProductsViewModel
import br.com.ticpass.pos.viewmodel.products.ProductsRefreshViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import androidx.core.view.isVisible
import java.math.BigInteger

@AndroidEntryPoint
class ProductsListScreen : Fragment(R.layout.fragment_products) {

    companion object {
        const val REQUEST_CART_UPDATE = 1001
        private const val SPLIT_EQUAL_REQUEST = 1002
        private const val SPLIT_MANUAL_REQUEST = 1003
    }

    private val productsViewModel: ProductsViewModel by viewModels()
    private val refreshViewModel: ProductsRefreshViewModel by viewModels()

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: CategoriesPagerAdapter
    private lateinit var paymentSheet: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var sessionPrefs: SharedPreferences
    private lateinit var userPrefs: SharedPreferences

    private var cartUpdatesObserver: Observer<Any>? = null
    private var loadingObserver: Observer<Boolean>? = null
    private var categoriesObserver: Observer<List<String>>? = null
    private var productsObserver: Observer<Map<String, List<Product>>>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        paymentSheet = view.findViewById(R.id.paymentSheetProducts)

        val forms = paymentSheet.findViewById<View>(R.id.payment_forms_container)
        forms.visibility = View.GONE

        val header = paymentSheet.findViewById<View>(R.id.payment_header_container)

        header.setOnClickListener {
            forms.visibility = if (forms.isVisible) View.GONE else View.VISIBLE
        }

        setupSwipeRefresh()
        setupTabLayout()

        cartUpdatesObserver = Observer {
            updatePaymentVisibility()
        }
        shoppingCartManager.cartUpdates.observe(viewLifecycleOwner, cartUpdatesObserver!!)

        viewPager.offscreenPageLimit = 1

        setupViewPager()
        setupPaymentMethods()

        loadingObserver = Observer { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
        productsViewModel.isLoading.observe(viewLifecycleOwner, loadingObserver!!)

        loadInitialData()
        updatePaymentVisibility()
    }

    private fun setupTabLayout() {
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        tabLayout.isTabIndicatorFullWidth = false
    }

    private fun setupViewPager() {
        categoriesObserver?.let { productsViewModel.categories.removeObserver(it) }
        productsObserver?.let { productsViewModel.productsByCategory.removeObserver(it) }

        categoriesObserver = Observer { categories ->
            productsObserver = Observer { productsMap ->
                pagerAdapter = CategoriesPagerAdapter(
                    requireActivity(),
                    categories,
                    productsMap
                )
                viewPager.adapter = pagerAdapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    val customView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_tab, null) as TextView
                    customView.text = categories[position]
                    tab.customView = customView
                }.attach()

                swipeRefreshLayout.isRefreshing = false
            }
            productsViewModel.productsByCategory.observe(viewLifecycleOwner, productsObserver!!)
        }
        productsViewModel.categories.observe(viewLifecycleOwner, categoriesObserver!!)
    }

    private fun loadInitialData() {
        productsViewModel.loadCategoriesWithProducts()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.design_default_color_primary,
            R.color.design_default_color_primary_dark,
            R.color.design_default_color_primary_variant
        )

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        swipeRefreshLayout.setDistanceToTriggerSync(200)
    }

    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val authToken = refreshViewModel.getAuthTokenFromPrefs(userPrefs)

                Log.d("ProductsListScreen", "Auth token: $authToken")

                if (authToken.isEmpty()) {
                    Log.d("ProductsListScreen", "Auth token not found!")
                    Toast.makeText(requireContext(), "Token de autenticação não encontrado", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                    return@launch
                }

                val eventId = refreshViewModel.getSelectedEventId()

                if (eventId == null) {
                    Log.d("ProductsListScreen", "No selected event found!")
                    Toast.makeText(requireContext(), "Nenhum evento selecionado", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                    return@launch
                }


                val menuId: String by lazy {
                    val value = sessionPrefs.all["selected_menu_id"]
                    when (value) {
                        is String -> value
                        is Int -> value.toString()
                        else -> ""
                    }
                }

                val success = refreshViewModel.refreshProducts(eventId, authToken, menuId)

                if (success) {
                    Log.d("ProductsListScreen", "Refresh bem-sucedido!")
                    productsViewModel.loadCategoriesWithProducts()
                    Toast.makeText(requireContext(), "Produtos atualizados", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("ProductsListScreen", "Refresh falhou!")
                    Toast.makeText(requireContext(), "Erro ao atualizar produtos", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                Log.e("ProductsListScreen", "Erro durante o refresh", e)
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updatePaymentVisibility() {
        val cart = shoppingCartManager.getCart()
        val hasItems = cart.items.isNotEmpty()

        paymentSheet.visibility = if (hasItems) View.VISIBLE else View.GONE

        if (hasItems) {
            updatePaymentInfo(cart)

            // Correto: btnOptions é LinearLayout
            paymentSheet.findViewById<LinearLayout>(R.id.btnOptions)?.setOnClickListener {
                showSplitBillDialog()
            }

            // Carrinho
            paymentSheet.findViewById<LinearLayout>(R.id.cart_container)?.setOnClickListener {
                val intent = Intent(requireContext(), ShoppingCartScreen::class.java)
                startActivity(intent)
            }
        }
    }


    private fun updatePaymentInfo(cart: ShoppingCartManager.ShoppingCart) {

        paymentSheet.findViewById<TextView>(R.id.tv_total_price)?.text =
            formatCurrency(cart.totalPrice)
    }

    private val paymentMethods = listOf(
        PaymentMethod("Dinheiro", R.drawable.cash, "cash"),
        PaymentMethod("Crédito", R.drawable.credit, "credit_card"),
        PaymentMethod("Débito", R.drawable.debit, "debit_card"),
        PaymentMethod("Pix", R.drawable.pix,  "pix"),
        PaymentMethod("Debug", R.drawable.icon,  "debug")
    )

    private fun setupPaymentMethods() {
        val container = paymentSheet.findViewById<GridLayout>(R.id.payment_methods_container)
        val columnCount = 2 // ou container.columnCount se já estiver definido no XML

        paymentMethods.forEachIndexed { index, method ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_payment_method, container, false)

            itemView.findViewById<ImageView>(R.id.iv_payment_icon).setImageResource(method.iconRes)
            itemView.findViewById<TextView>(R.id.tv_payment_name).text = method.name

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % columnCount, 1f)
                rowSpec = GridLayout.spec(index / columnCount)
                setMargins(8, 8, 8, 8)
            }

            itemView.setOnClickListener {
                val intent = Intent(requireContext(), PaymentScreen::class.java).apply {
                    putExtra("payment_type", method.value)
                }
                startActivity(intent)
            }

            container.addView(itemView, params)
        }
    }

    private fun formatCurrency(valueInCents: BigInteger): String {
        val valueInReais = valueInCents.toDouble() / 100000
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(valueInReais)
    }

    override fun onResume() {
        super.onResume()
        paymentSheet.findViewById<View>(R.id.payment_forms_container).visibility = View.GONE
        updatePaymentVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        cartUpdatesObserver?.let {
            shoppingCartManager.cartUpdates.removeObserver(it)
            cartUpdatesObserver = null
        }

        loadingObserver?.let {
            productsViewModel.isLoading.removeObserver(it)
            loadingObserver = null
        }

        categoriesObserver?.let {
            productsViewModel.categories.removeObserver(it)
            categoriesObserver = null
        }

        productsObserver?.let {
            productsViewModel.productsByCategory.removeObserver(it)
            productsObserver = null
        }

        viewPager.adapter = null
        cartUpdatesObserver = null
        loadingObserver = null
        categoriesObserver = null
        productsObserver = null
        viewPager.adapter = null
    }

    private fun showSplitBillDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Dividir conta")
            .setMessage("Como deseja dividir?")
            .setPositiveButton("Partes iguais") { dialog, _ ->
                startActivity(Intent(requireContext(), SplitEqualActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("Valor manual") { dialog, _ ->
                startActivity(Intent(requireContext(), SplitManualActivity::class.java))
                dialog.dismiss()
            }
            .show()
    }
}