package br.com.ticpass.pos.presentation.shoppingCart.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ActivityShoppingCartBinding
import br.com.ticpass.pos.domain.shoppingCart.model.CartItemModel
import br.com.ticpass.pos.presentation.payment.fragments.PaymentSheetFragment
import br.com.ticpass.pos.presentation.shared.activities.BaseActivity
import br.com.ticpass.pos.presentation.shoppingCart.adapters.CartAdapter
import br.com.ticpass.pos.presentation.shoppingCart.viewmodels.CartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShoppingCartActivity : BaseActivity(),
    PaymentSheetFragment.PaymentSheetHeightListener {

    private lateinit var binding: ActivityShoppingCartBinding
    private val viewModel: CartViewModel by viewModels()

    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botão voltar
        binding.btnBack.setOnClickListener { finish() }

        // Botão limpar carrinho
        binding.btnClearCart.setOnClickListener {
            showClearCartDialog()
        }

        // Adapter
        cartAdapter = CartAdapter(
            onIncrease = { item -> viewModel.addProduct(item.product) },
            onDecrease = { item -> viewModel.removeProduct(item.product) },
            onRemove   = { item -> showRemoveItemDialog(item) }
        )

        binding.recyclerCart.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(this@ShoppingCartActivity)
        }

        // Adiciona o PaymentSheetFragment expandido
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.paymentSheetContainer,
                    PaymentSheetFragment.newInstance(expanded = true)
                )
                .commit()
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

    /** Ajusta o padding inferior da lista conforme a altura atual do PaymentSheet */
    override fun onPaymentSheetHeightChanged(heightPx: Int) {
        binding.recyclerCart.setPadding(
            binding.recyclerCart.paddingLeft,
            binding.recyclerCart.paddingTop,
            binding.recyclerCart.paddingRight,
            heightPx
        )
        binding.recyclerCart.clipToPadding = false
    }

    /** Dialog para remover um produto específico do carrinho */
    private fun showRemoveItemDialog(cartItem: CartItemModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_remove_cart_item, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)
        val btnNo = dialogView.findViewById<AppCompatButton>(R.id.btnNo)
        val btnYes = dialogView.findViewById<AppCompatButton>(R.id.btnYes)

        // Personaliza a mensagem com o nome do produto
        tvTitle.text = "Remover produto"
        tvMessage.text = "Deseja remover \"${cartItem.product.name}\" do carrinho?"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        btnYes.setOnClickListener {
            viewModel.removeAllProductItems(cartItem.product)
            dialog.dismiss()
        }

        dialog.show()
    }

    /** Dialog para limpar todo o carrinho */
    private fun showClearCartDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clear_cart, null)

        val btnNo = dialogView.findViewById<AppCompatButton>(R.id.btnNo)
        val btnYes = dialogView.findViewById<AppCompatButton>(R.id.btnYes)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        btnYes.setOnClickListener {
            viewModel.clearCart()
            dialog.dismiss()
        }

        dialog.show()
    }
}