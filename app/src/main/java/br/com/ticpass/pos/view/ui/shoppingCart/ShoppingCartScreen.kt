package br.com.ticpass.pos.view.ui.shoppingCart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.PaymentActivity
import br.com.ticpass.pos.data.room.entity.CartItem
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.view.ui.payment.PaymentScreen
import br.com.ticpass.pos.view.ui.payment.adapter.PaymentAdapter
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import br.com.ticpass.pos.view.ui.shoppingCart.adapter.ShoppingCartAdapter
import br.com.ticpass.pos.viewmodel.payment.PaymentMethod
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ShoppingCartScreen : AppCompatActivity() {
    @Inject lateinit var productRepository: ProductRepository
    @Inject lateinit var shoppingCartManager: ShoppingCartManager

    private lateinit var adapter: ShoppingCartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyCart: TextView
    private lateinit var btnBack: MaterialButton

    private lateinit var paymentSheet: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)
        paymentSheet = findViewById(R.id.paymentSheet)


        recyclerView = findViewById(R.id.recyclerCart)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { onBackPressed() }

        setupRecyclerView()
        setupObservers()
        loadInitialData()
        setupPaymentSheet()
        updatePaymentVisibility()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingCartAdapter { item, newQuantity ->
            shoppingCartManager.updateItem(item.product.id, newQuantity)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        shoppingCartManager.cartUpdates.observe(this) {
            loadInitialData()
            updatePaymentVisibility()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            val cart = shoppingCartManager.getCart()
            updateUI(cart)
        }
    }

    fun getTotalItems(): Int {
        return shoppingCartManager.getCart().items.values.sum()
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

        paymentSheet.findViewById<ImageView>(R.id.btnClearAll)?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Limpar carrinho")
                .setMessage("Deseja remover todos os itens do carrinho?")
                .setPositiveButton("Sim") { dialog, _ ->
                    shoppingCartManager.clearCart()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


    }

    private fun updatePaymentInfo(cart: ShoppingCartManager.ShoppingCart) {
        paymentSheet.findViewById<TextView>(R.id.tv_items_count)?.text =
            "${cart.items.values.sum()} itens"

        paymentSheet.findViewById<TextView>(R.id.tv_total_price)?.text =
            formatCurrency(cart.totalPrice.toDouble())
    }

    private fun setupPaymentSheet() {
        val paymentMethods = listOf(
            PaymentMethod("Dinheiro", R.drawable.cash, "cash"),
            PaymentMethod("Crédito", R.drawable.credit, "credit_card"),
            PaymentMethod("Débito", R.drawable.debit, "debit_card"),
            PaymentMethod("Vale Refeição", R.drawable.vr, "vr"),
            PaymentMethod("Pix", R.drawable.pix,  "pix")
        )

        val recyclerView = paymentSheet.findViewById<RecyclerView>(R.id.rv_payment_methods)
        if (recyclerView.adapter == null) {
            recyclerView.layoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            recyclerView.adapter = PaymentAdapter(paymentMethods) { method ->
                val intent = Intent(this, PaymentScreen::class.java)
                intent.putExtra("payment_type", method.value)
                startActivity(intent)
            }
            recyclerView.addItemDecoration(ProductsListScreen.HorizontalSpaceItemDecoration(16))
        }
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }


    private fun updateUI(cart: ShoppingCartManager.ShoppingCart) {
        lifecycleScope.launch {
            val cartItems = mutableListOf<CartItem>()

            cart.items.forEach { (productId, quantity) ->
                val product = withContext(Dispatchers.IO) {
                    productRepository.getById(productId)
                }
                product?.let {
                    cartItems.add(CartItem(it, quantity))
                }
            }

            adapter.submitList(cartItems)

            if (cart.items.isEmpty()) {
                tvEmptyCart.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmptyCart.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            updatePaymentVisibility()
        }
    }
}