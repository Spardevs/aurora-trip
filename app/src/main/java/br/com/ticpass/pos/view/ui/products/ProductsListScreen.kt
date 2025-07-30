package br.com.ticpass.pos.view.ui.products

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.payment.PaymentScreen
import br.com.ticpass.pos.view.ui.payment.adapter.PaymentAdapter
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.viewmodel.payment.PaymentMethod
import br.com.ticpass.pos.viewmodel.products.ProductsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class ProductsListScreen : Fragment(R.layout.fragment_products) {
    private val viewModel: ProductsViewModel by viewModels()
    @Inject lateinit var shoppingCartManager: ShoppingCartManager
    private lateinit var tabLayout: TabLayout
    private lateinit var recycler: RecyclerView

    private lateinit var paymentSheet: View

    private lateinit var adapter: ProductsAdapter

    class HorizontalSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.left = space
            }
        }
    }

    companion object {
        const val REQUEST_CART_UPDATE = 1001
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabCategories)
        recycler = view.findViewById(R.id.rvProducts)
        paymentSheet = view.findViewById(R.id.paymentSheet)

        adapter = ProductsAdapter(shoppingCartManager) { product ->
            shoppingCartManager.addItem(product.id)
            updatePaymentVisibility()
        }

        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        shoppingCartManager.cartUpdates.observe(viewLifecycleOwner) {
            updatePaymentVisibility()
            adapter.notifyDataSetChanged()
        }

        setupTabLayout()
        setupPaymentMethods()
    }

    private fun updatePaymentVisibility() {
        val cart = shoppingCartManager.getCart()
        val hasItems = cart.items.isNotEmpty()

        if (hasItems) {
            paymentSheet.visibility = View.VISIBLE
            updatePaymentInfo(cart)
        } else {
            paymentSheet.visibility = View.GONE
        }

        paymentSheet.findViewById<ImageButton>(R.id.btnClearAll).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
        PaymentMethod("Dinheiro", R.drawable.cash),
        PaymentMethod("Crédito", R.drawable.credit),
        PaymentMethod("Débito", R.drawable.debit),
        PaymentMethod("Vale Refeição", R.drawable.vr),
        PaymentMethod("Pix", R.drawable.pix)
    )
    private fun setupPaymentMethods() {
        val recyclerView = paymentSheet.findViewById<RecyclerView>(R.id.rv_payment_methods)

        if (recyclerView.adapter == null) {
            recyclerView.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            recyclerView.adapter = PaymentAdapter(paymentMethods) { method ->
                val intent = Intent(requireContext(), PaymentScreen::class.java)
                intent.putExtra("payment_type", method.name)
                startActivity(intent)
            }
            recyclerView.addItemDecoration(HorizontalSpaceItemDecoration(16))
        }
    }

    private fun setupTabLayout() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            tabLayout.removeAllTabs()
            categories.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
            if (categories.isNotEmpty()) onCategorySelected(categories[0])
        }

        viewModel.productsByCategory.observe(viewLifecycleOwner) { productsMap ->
            val selectedTab = tabLayout.selectedTabPosition
            val category = viewModel.categories.value?.getOrNull(selectedTab)
            adapter.submitList(category?.let { productsMap[it] } ?: emptyList())
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                onCategorySelected(tab.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        viewModel.loadCategoriesWithProducts()
        adapter.notifyDataSetChanged()
        updatePaymentVisibility()
    }

    @SuppressLint("NotifyDataSetChanged")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CART_UPDATE) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun onCategorySelected(cat: String) {
        adapter.submitList(viewModel.productsByCategory.value?.get(cat) ?: emptyList())
    }
}

