package br.com.ticpass.pos.data.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.entity.CartItem
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.view.ui.shoppingCart.adapter.ShoppingCartAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShoppingCartActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingCartAdapter
    private lateinit var productRepository: ProductRepository
    private lateinit var shoppingCartManager: ShoppingCartManager

    @Inject
    lateinit var injectedShoppingCartManager: ShoppingCartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        recyclerView = findViewById(R.id.recyclerCart)

        adapter = ShoppingCartAdapter { cartItem, newQuantity ->
            shoppingCartManager.updateItem(cartItem.product.id, newQuantity)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        shoppingCartManager.cartUpdates.observe(this) {
            loadCartItems()
        }

        loadCartItems()
    }

    override fun onBackPressed() {
        shoppingCartManager.notifyCartUpdated()
        super.onBackPressed()
    }

    private fun loadCartItems() {
        lifecycleScope.launch {
            val cart = shoppingCartManager.getCart()
            val cartItems = mutableListOf<CartItem>()

            cart.items.forEach { (productId, quantity) ->
                val product = withContext(Dispatchers.IO) {
                    productRepository.getById(productId.toString())
                }
                product?.let {
                    cartItems.add(CartItem(it, quantity))
                }
            }

            adapter.submitList(cartItems)
        }
    }


    private suspend fun attCartItems(): List<CartItem> {
        val map: Map<String, Int> = shoppingCartManager.getAllItems(this@ShoppingCartActivity)

        return map.mapNotNull { (productId, quantity) ->
            val entity = productRepository.getById(productId.toString())
            entity?.let { CartItem(it, quantity) }
        }
    }
    private suspend fun getCartItems(): List<CartItem> {
        val itemsMap = shoppingCartManager.getAllItems(this@ShoppingCartActivity)
        return itemsMap.mapNotNull { (productId, quantity) ->
            val product = withContext(Dispatchers.IO) {
                productRepository.getById(productId.toString())
            }
            product?.let { CartItem(it, quantity) }
        }
    }

}
