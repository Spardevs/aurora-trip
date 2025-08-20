// ProductsListScreen.kt
package br.com.ticpass.pos.view.ui.products

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    // REMOVIDO: private lateinit var tvError: TextView

    // Preferências
    private lateinit var sessionPrefs: SharedPreferences
    private lateinit var userPrefs: SharedPreferences

    // Observers
    private var cartUpdatesObserver: Observer<Any>? = null
    private var loadingObserver: Observer<Boolean>? = null
    private var categoriesObserver: Observer<List<String>>? = null
    private var productsObserver: Observer<Map<String, List<Product>>>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Inicializar as preferências
        sessionPrefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
        userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        tabLayout = view.findViewById(R.id.tabCategories)
        viewPager = view.findViewById(R.id.viewPager)
        paymentSheet = view.findViewById(R.id.paymentSheet)
        // REMOVIDO: tvError = view.findViewById(R.id.tvError)

        setupSwipeRefresh()
        setupTabLayout()

        cartUpdatesObserver = Observer<Any> {
            updatePaymentVisibility()
        }
        shoppingCartManager.cartUpdates.observe(viewLifecycleOwner, cartUpdatesObserver!!)

        viewPager.offscreenPageLimit = 1

        setupViewPager()
        setupPaymentMethods()

        loadingObserver = Observer<Boolean> { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
            // REMOVIDO: if (isLoading) {
            // REMOVIDO:     tvError.visibility = View.GONE
            // REMOVIDO: }
        }
        productsViewModel.isLoading.observe(viewLifecycleOwner, loadingObserver!!)

        // Carregar dados iniciais
        loadInitialData()
        updatePaymentVisibility()
    }

    private fun setupTabLayout() {
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        tabLayout.isTabIndicatorFullWidth = false
    }

    private fun setupViewPager() {
        // Remover observers antigos se existirem
        categoriesObserver?.let { productsViewModel.categories.removeObserver(it) }
        productsObserver?.let { productsViewModel.productsByCategory.removeObserver(it) }

        categoriesObserver = Observer<List<String>> { categories ->
            productsObserver = Observer<Map<String, List<Product>>> { productsMap ->
                pagerAdapter = CategoriesPagerAdapter(
                    requireActivity(),
                    categories,
                    productsMap
                )
                viewPager.adapter = pagerAdapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = categories[position]
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

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.design_default_color_primary,
            R.color.design_default_color_primary_dark,
            R.color.design_default_color_primary_variant
        )

        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        swipeRefreshLayout.setDistanceToTriggerSync(120)
    }

    private fun refreshData() {
        val eventId = refreshViewModel.getEventIdFromPrefs(sessionPrefs)
        val authToken = refreshViewModel.getAuthTokenFromPrefs(userPrefs)

        if (eventId.isNotEmpty() && authToken.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val success = refreshViewModel.refreshProducts(eventId, authToken)
                if (success) {
                    productsViewModel.loadCategoriesWithProducts()
                    Toast.makeText(requireContext(), "Produtos atualizados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Erro ao atualizar produtos", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                    // REMOVIDO: tvError.visibility = View.VISIBLE
                    // REMOVIDO: tvError.text = "Erro ao atualizar. Toque para tentar novamente."
                }
            }
        } else {
            productsViewModel.loadCategoriesWithProducts()
            swipeRefreshLayout.isRefreshing = false
            // REMOVIDO: tvError.visibility = View.VISIBLE
            // REMOVIDO: tvError.text = "Sessão expirada. Faça login novamente."
        }
    }

    private fun updatePaymentVisibility() {
        val cart = shoppingCartManager.getCart()
        val hasItems = cart.items.isNotEmpty()

        if (hasItems) {
            paymentSheet.visibility = View.VISIBLE
            updatePaymentInfo(cart)

            paymentSheet.findViewById<ImageButton>(R.id.btnOptions)?.setOnClickListener {
                showSplitBillDialog()
            }
        } else {
            paymentSheet.visibility = View.GONE
        }

        paymentSheet.findViewById<ImageButton>(R.id.btnClearAll).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Limpar carrinho")
                .setMessage("Deseja remover todos os itens do carrinho?")
                .setPositiveButton("Sim") { dialog, _ ->
                    shoppingCartManager.clearCart()
                    Toast.makeText(requireContext(), "Carrinho limpo", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun updatePaymentInfo(cart: ShoppingCartManager.ShoppingCart) {
        paymentSheet.findViewById<TextView>(R.id.tv_items_count).text =
            "${cart.items.values.sum()} itens"

        paymentSheet.findViewById<TextView>(R.id.tv_total_price).text =
            formatCurrency(cart.totalPrice.toDouble())
    }

    private val paymentMethods = listOf(
        PaymentMethod("Dinheiro", R.drawable.cash, "cash"),
        PaymentMethod("Crédito", R.drawable.credit, "credit_card"),
        PaymentMethod("Débito", R.drawable.debit, "debit_card"),
        PaymentMethod("VR", R.drawable.vr, "vr"),
        PaymentMethod("Pix", R.drawable.pix,  "pix")
    )

    private fun setupPaymentMethods() {
        val container = paymentSheet.findViewById<GridLayout>(R.id.payment_methods_container)
        container.removeAllViews()

        paymentMethods.forEachIndexed { index, method ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_payment_method, container, false)

            itemView.findViewById<ImageView>(R.id.iv_payment_icon).setImageResource(method.iconRes)
            itemView.findViewById<TextView>(R.id.tv_payment_name).text = method.name

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % 3, 1f)
                rowSpec = GridLayout.spec(index / 3)
                setMargins(8, 8, 8, 8)
            }

            itemView.setOnClickListener {
                val intent = Intent(requireContext(), PaymentScreen::class.java)
                intent.putExtra("payment_type", method.value)
                startActivity(intent)
            }

            container.addView(itemView, params)
        }
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }

    override fun onResume() {
        super.onResume()
        updatePaymentVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Remover todos os observers
        cartUpdatesObserver?.let {
            shoppingCartManager.cartUpdates.removeObserver(it)
        }

        loadingObserver?.let {
            productsViewModel.isLoading.removeObserver(it)
        }

        categoriesObserver?.let {
            productsViewModel.categories.removeObserver(it)
        }

        productsObserver?.let {
            productsViewModel.productsByCategory.removeObserver(it)
        }

        // Limpar referências
        cartUpdatesObserver = null
        loadingObserver = null
        categoriesObserver = null
        productsObserver = null

        // Limpar adapter do ViewPager
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