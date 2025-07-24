package br.com.ticpass.pos.view.ui.shoppingCart

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.view.ui.shoppingCart.adapter.ShoppingCartAdapter
import kotlinx.coroutines.launch
import androidx.core.content.edit
import br.com.ticpass.pos.view.ui.payment.PaymentBottomSheet

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
)

class ShoppingCartScreen : AppCompatActivity() {
    private lateinit var adapter: ShoppingCartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        val backButton = findViewById<Button>(R.id.btnBack)
        val clearAllButton = findViewById<Button>(R.id.btnClearAll)
        val recycler = findViewById<RecyclerView>(R.id.recyclerCart)

        adapter = ShoppingCartAdapter { item, newQuantity ->
            val prefs = getSharedPreferences("ShoppingCart", Context.MODE_PRIVATE)
            if (newQuantity <= 0) {
                prefs.edit { remove(item.product.id) }
            } else {
                prefs.edit { putInt(item.product.id, newQuantity) }
            }
            loadCartItems()
        }
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        loadCartItems()

        backButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }


        clearAllButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Limpar Carrinho")
                .setMessage("Tem certeza que deseja remover todos os produtos?")
                .setPositiveButton("Sim") { _, _ ->
                    val prefs = getSharedPreferences("ShoppingCart", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    loadCartItems()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun loadCartItems() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("ShoppingCart", Context.MODE_PRIVATE)
            val dao = AppDatabase.getInstance(this@ShoppingCartScreen).productDao()
            val emptyText = findViewById<TextView>(R.id.tvEmptyCart)
            val totalText = findViewById<TextView>(R.id.tvTotalValue)
            val footer = findViewById<LinearLayout>(R.id.footerTotal)
            val footerBg = findViewById<View>(R.id.footerBackground)
            val confirm = findViewById<Button>(R.id.btnConfirm)

            val items = prefs.all.mapNotNull { (key, value) ->
                val product = dao.getById(key)
                val quantity = (value as? Int) ?: return@mapNotNull null
                product?.let { CartItem(it, quantity) }
            }

            adapter.submitList(items)

            val isEmpty = items.isEmpty()
            emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            footerBg.visibility = if (isEmpty) View.GONE else View.VISIBLE
            footer.visibility = if (isEmpty) View.GONE else View.VISIBLE
            confirm.visibility = if (isEmpty) View.GONE else View.VISIBLE

            val totalCents = items.sumOf { it.product.price * it.quantity }
            val totalFormatted = formatCurrency(totalCents)
            totalText.text = totalFormatted

            confirm.setOnClickListener {
                PaymentBottomSheet()
                    .show(supportFragmentManager, "PaymentBottomSheet")
            }
        }
    }


    private fun formatCurrency(value: Long): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
        return formatter.format(value / 100.0)
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
