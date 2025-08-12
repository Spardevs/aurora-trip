package br.com.ticpass.pos.view.ui.products

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.SplitEqualActivity
import br.com.ticpass.pos.data.activity.SplitManualActivity
import br.com.ticpass.pos.view.ui.payment.PaymentScreen
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.viewmodel.payment.PaymentMethod
import br.com.ticpass.pos.viewmodel.products.ProductsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs


@AndroidEntryPoint
class ProductsListScreen : Fragment(R.layout.fragment_products) {
    private val viewModel: ProductsViewModel by viewModels()
    @Inject lateinit var shoppingCartManager: ShoppingCartManager
    private lateinit var tabLayout: TabLayout
    private lateinit var recycler: RecyclerView

    private lateinit var gestureDetector: GestureDetector

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
        private const val SPLIT_EQUAL_REQUEST = 1002
        private const val SPLIT_MANUAL_REQUEST = 1003
    }

    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabCategories)
        recycler = view.findViewById(R.id.rvProducts)
        paymentSheet = view.findViewById(R.id.paymentSheet)

        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) && abs(diffX) > 100 && abs(velocityX) > 100) {
                    if (diffX > 0) {
                        moveToPreviousCategory()
                    } else {
                        moveToNextCategory()
                    }
                    return true
                }
                return false
            }
        })

        recycler.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            false
        }


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

            paymentSheet.findViewById<ImageButton>(R.id.btnOptions)?.setOnClickListener {
                showSplitBillDialog()
            }
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

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SPLIT_EQUAL_REQUEST -> {
                    val dividedValue = data?.getDoubleExtra("divided_value", 0.0) ?: 0.0
                    val peopleCount = data?.getIntExtra("people_count", 1) ?: 1
                    showDivisionResult(dividedValue, peopleCount)
                }
                SPLIT_MANUAL_REQUEST -> {
                    val subtractedValues = data?.getDoubleArrayExtra("subtracted_values")?.toList() ?: emptyList()
                    val remainingValue = data?.getDoubleExtra("remaining_value", 0.0) ?: 0.0

                    val message = buildString {
                        append("Valores subtraídos:\n")
                        subtractedValues.forEachIndexed { index, value ->
                            append("${index + 1}. ${formatCurrency(value)}\n")
                        }
                        append("\nValor restante: ${formatCurrency(remainingValue)}")
                    }

                    AlertDialog.Builder(requireContext())
                        .setTitle("Divisão manual realizada")
                        .setMessage(message)
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }


    private fun onCategorySelected(cat: String) {
        adapter.submitList(viewModel.productsByCategory.value?.get(cat) ?: emptyList())
    }

    private fun moveToNextCategory() {
        viewModel.categories.value?.let { categories ->
            val currentPos = tabLayout.selectedTabPosition
            if (currentPos < categories.size - 1) {
                tabLayout.getTabAt(currentPos + 1)?.select()
            } else {
                tabLayout.getTabAt(0)?.select()
            }
        }
    }

    private fun moveToPreviousCategory() {
        viewModel.categories.value?.let { categories ->
            val currentPos = tabLayout.selectedTabPosition
            if (currentPos > 0) {
                tabLayout.getTabAt(currentPos - 1)?.select()
            } else {
                tabLayout.getTabAt(categories.size - 1)?.select()
            }
        }
    }

    private fun showSplitBillDialog() {
        AlertDialog.Builder(requireContext()) // Use requireContext() instead of this
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

    private fun showDivisionResult(dividedValue: Double, peopleCount: Int) {
        val message = "Dividido em $peopleCount partes\n" +
                "Valor por parte: ${formatCurrency(dividedValue)}"

        AlertDialog.Builder(requireContext())
            .setTitle("Divisão realizada")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

}

