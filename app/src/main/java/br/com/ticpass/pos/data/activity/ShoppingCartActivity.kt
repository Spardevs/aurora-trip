package br.com.ticpass.pos.data.activity

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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

class ShoppingCartActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingCartAdapter
    @Inject lateinit var productRepository: ProductRepository
    @Inject lateinit var shoppingCartManager: ShoppingCartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        recyclerView = findViewById(R.id.recyclerCart)

        adapter = ShoppingCartAdapter(
            onQuantityChange = { cartItem, newQuantity ->
                shoppingCartManager.updateItem(cartItem.product.id, newQuantity)
            },
            onObservationClick = { cartItem ->
                showObservationDialog(cartItem)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        shoppingCartManager.cartUpdates.observe(this) {
            loadCartItems()
        }

        loadCartItems()
    }

    private fun showObservationDialog(item: CartItem) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Observação do produto")
            .setMessage("Digite uma observação para ${item.product.name}")
            .setView(R.layout.dialog_observation)
            .setPositiveButton("Salvar") { dialog, _ ->
                val input = (dialog as AlertDialog).findViewById<EditText>(R.id.btObservation)
                val observation = input?.text?.toString()?.trim() ?: ""
                shoppingCartManager.updateObservation(item.product.id, observation)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Limpar") { dialog, _ ->
                shoppingCartManager.updateObservation(item.product.id, "")
            }
            .create()

        dialog.setOnShowListener {
            val currentObservation = shoppingCartManager.getObservation(item.product.id)
            val input = dialog.findViewById<EditText>(R.id.btObservation)
            input?.setText(currentObservation)
            input?.requestFocus()
        }

        dialog.show()
    }

    private fun loadCartItems() {
        lifecycleScope.launch {
            val cart = shoppingCartManager.getCart()
            val cartItems = mutableListOf<CartItem>()

            cart.items.forEach { (productId, quantity) ->
                val product = withContext(Dispatchers.IO) {
                    productRepository.getById(productId)
                }
                product?.let {
                    val observation = cart.observations[productId]
                    cartItems.add(CartItem(it, quantity, observation))
                }
            }

            adapter.submitList(cartItems)
        }
    }

    override fun onBackPressed() {
        shoppingCartManager.notifyCartUpdated()
        super.onBackPressed()
    }

}