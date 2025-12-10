package br.com.ticpass.pos.presentation.shoppingCart.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.ticpass.pos.databinding.ActivityShoppingCartBinding
import br.com.ticpass.pos.presentation.shared.activities.BaseActivity
import br.com.ticpass.pos.presentation.shoppingCart.adapters.CartAdapter
import br.com.ticpass.pos.presentation.shoppingCart.viewmodels.CartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShoppingCartActivity : BaseActivity() {

    private lateinit var binding: ActivityShoppingCartBinding
    private val viewModel: CartViewModel by viewModels()

    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botão voltar
        binding.btnBack.setOnClickListener { finish() }

        // Adapter
        cartAdapter = CartAdapter(
            onIncrease = { item -> viewModel.addProduct(item.product) },
            onDecrease = { item -> viewModel.removeProduct(item.product) }
        )

        binding.recyclerCart.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(this@ShoppingCartActivity)
        }

        // Observa lista de itens
        lifecycleScope.launchWhenStarted {
            viewModel.cartItems.collect { items ->
                cartAdapter.submitList(items)
                binding.tvEmptyCart.visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // (Opcional) observar total, quantidade etc.
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                // atualizar total na tela se você quiser
            }
        }
    }
}