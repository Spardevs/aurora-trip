package br.com.ticpass.pos.view.ui.shoppingCart

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.entity.CartItem
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.view.ui.shoppingCart.adapter.ShoppingCartAdapter
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ShoppingCartScreen : AppCompatActivity() {
    @Inject lateinit var productRepository: ProductRepository
    @Inject lateinit var shoppingCartManager: ShoppingCartManager

    private lateinit var adapter: ShoppingCartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyCart: TextView
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        recyclerView = findViewById(R.id.recyclerCart)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { onBackPressed() }

        setupRecyclerView()
        setupObservers()
        loadInitialData()
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
        }
    }
}