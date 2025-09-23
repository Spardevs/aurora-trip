package br.com.ticpass.pos.view.ui.shoppingCart

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.activity.SplitEqualActivity
import br.com.ticpass.pos.data.activity.SplitManualActivity
import br.com.ticpass.pos.data.room.entity.CartItem
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.view.ui.payment.PaymentScreen
import br.com.ticpass.pos.view.ui.shoppingCart.adapter.ShoppingCartAdapter
import br.com.ticpass.pos.viewmodel.payment.PaymentMethod
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ShoppingCartScreen : BaseActivity() {
    @Inject lateinit var productRepository: ProductRepository
    @Inject lateinit var shoppingCartManager: ShoppingCartManager


    private lateinit var adapter: ShoppingCartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyCart: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnOptionCart: ImageButton
    private lateinit var paymentSheet: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        paymentSheet = findViewById(R.id.paymentSheet)
        recyclerView = findViewById(R.id.recyclerCart)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        btnBack = findViewById(R.id.btnBack)
        btnOptionCart = findViewById(R.id.btnOptionCart)

        btnBack.setOnClickListener { onBackPressed() }

        btnOptionCart.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_cart_options, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_clear_cart -> {
                        showClearCartDialog()
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        val header = paymentSheet.findViewById<View>(R.id.payment_header_container)
        val forms = paymentSheet.findViewById<View>(R.id.payment_forms_container)
        val tvSubTotalContainer = paymentSheet.findViewById<View>(R.id.ll_sub_total)
        val tvTotalCommissionContainer = paymentSheet.findViewById<View>(R.id.ll_total_commission)
        val cartContainer = paymentSheet.findViewById<LinearLayout>(R.id.cart_container)

        header.setOnClickListener {
            if (forms.visibility == View.VISIBLE) {
                forms.visibility = View.GONE
                tvSubTotalContainer.visibility = View.VISIBLE
                tvTotalCommissionContainer.visibility = View.VISIBLE
                cartContainer.visibility = View.GONE
            } else {
                tvSubTotalContainer.visibility = View.VISIBLE
                tvTotalCommissionContainer.visibility = View.VISIBLE
                cartContainer.visibility = View.GONE
                val cart = shoppingCartManager.getCart()
                updatePaymentInfo(cart)
                forms.visibility = View.VISIBLE
            }
        }

        setupShoppingCart()
        setupPaymentMethods()
        updatePaymentVisibility()
    }

    private fun setupShoppingCart() {
        adapter = ShoppingCartAdapter(
            onQuantityChange = { item, newQuantity ->
                shoppingCartManager.updateItem(item.product.id, newQuantity)
            },
            onObservationClick = { item ->
                showObservationDialog(item)
            },
            onMinusClick = { item ->
                if (item.quantity == 1) {
                    shoppingCartManager.updateItem(item.product.id, 0)
                } else {
                    shoppingCartManager.updateItem(item.product.id, item.quantity - 1)
                }
            },
            onMinusLongClick = { item ->
                shoppingCartManager.deleteItem(item.product.id)
            },
            getProductCommission = { productId ->
                shoppingCartManager.getProductCommission(productId)?.toLong() ?: 0L
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 1)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(GridSpacingItemDecoration(3,
            resources.getDimensionPixelSize(R.dimen.grid_spacing), true))

        shoppingCartManager.cartUpdates.observe(this) {
            loadCartData()
            updatePaymentVisibility()
        }

        loadCartData()
    }

    private fun loadCartData() {
        lifecycleScope.launch {
            val cart = shoppingCartManager.getCart()
            updateCartUI(cart)
        }
    }

    private fun updateCartUI(cart: ShoppingCartManager.ShoppingCart) {
        lifecycleScope.launch {
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

            if (cart.items.isEmpty()) {
                tvEmptyCart.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmptyCart.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private val paymentMethods = listOf(
        PaymentMethod("Dinheiro", R.drawable.cash, "cash"),
        PaymentMethod("Crédito", R.drawable.credit, "credit_card"),
        PaymentMethod("Débito", R.drawable.debit, "debit_card"),
        PaymentMethod("Pix", R.drawable.pix,  "pix"),
        PaymentMethod("Debug", R.drawable.icon,  "debug")

    )
    private fun setupPaymentMethods() {
        val container = paymentSheet.findViewById<GridLayout>(R.id.payment_methods_container)
        container.removeAllViews()
        val columnCount = 2 // número de colunas definido

        paymentMethods.forEachIndexed { index, method ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_payment_method, container, false)

            itemView.findViewById<ImageView>(R.id.iv_payment_icon).setImageResource(method.iconRes)
            itemView.findViewById<TextView>(R.id.tv_payment_name).text = method.name

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % columnCount, 1f)
                rowSpec = GridLayout.spec(index / columnCount)
                setMargins(8, 8, 8, 8)
            }

            itemView.setOnClickListener {
                val intent = Intent(this, PaymentScreen::class.java)
                intent.putExtra("payment_type", method.value)
                intent.putExtra("show_cart_button", false)
                startActivity(intent)
            }

            container.addView(itemView, params)
        }
    }

    private fun showSplitBillDialog() {
        AlertDialog.Builder(this)
            .setTitle("Dividir conta")
            .setMessage("Como deseja dividir?")
            .setPositiveButton("Partes iguais") { dialog, _ ->
                startActivity(Intent(this, SplitEqualActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("Valor manual") { dialog, _ ->
                startActivity(Intent(this, SplitManualActivity::class.java))
                dialog.dismiss()
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
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

                    AlertDialog.Builder(this)
                        .setTitle("Divisão manual realizada")
                        .setMessage(message)
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }

    private fun showDivisionResult(dividedValue: Double, peopleCount: Int) {
        val message = "Dividido em $peopleCount partes\n" +
                "Valor por parte: ${formatCurrency(dividedValue)}"

        AlertDialog.Builder(this)
            .setTitle("Divisão realizada")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        private const val SPLIT_EQUAL_REQUEST = 1001
        private const val SPLIT_MANUAL_REQUEST = 1002
    }

    private fun updatePaymentVisibility() {
        val cart = shoppingCartManager.getCart()
        val hasItems = cart.items.isNotEmpty()

        paymentSheet.visibility = if (hasItems) View.VISIBLE else View.GONE

        if (hasItems) {
            updatePaymentInfo(cart)

            paymentSheet.findViewById<LinearLayout>(R.id.btnOptions)?.setOnClickListener {
                showSplitBillDialog()
            }

            // Aqui escondemos o carrinho e mostramos os totais ao abrir o paymentSheet
            paymentSheet.findViewById<LinearLayout>(R.id.cart_container)?.visibility = View.GONE
            paymentSheet.findViewById<View>(R.id.ll_sub_total)?.visibility = View.VISIBLE
            paymentSheet.findViewById<View>(R.id.ll_total_commission)?.visibility = View.VISIBLE
        }
    }

    private fun updatePaymentInfo(cart: ShoppingCartManager.ShoppingCart) {
        // Atualiza total principal imediatamente
        paymentSheet.findViewById<TextView>(R.id.tv_total_price)?.text =
            formatCurrency(cart.totalPrice.toDouble())

        lifecycleScope.launch {
            var productsTotal = 0.0

            for ((productId, qty) in cart.items) {
                val product = withContext(Dispatchers.IO) {
                    productRepository.getById(productId)
                }
                product?.let {
                    productsTotal += it.price * qty
                }
            }

            val commission = (cart.totalPrice.toDouble() - productsTotal).coerceAtLeast(0.0)

            paymentSheet.findViewById<TextView>(R.id.tv_sub_total)?.text =
                formatCurrency(productsTotal)
            paymentSheet.findViewById<TextView>(R.id.tv_total_commission)?.text =
                formatCurrency(commission)
        }
    }

    private fun showClearCartDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clear_cart, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val btnNo = dialogView.findViewById<Button>(R.id.btnNo)
        val btnYes = dialogView.findViewById<Button>(R.id.btnYes)

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        btnYes.setOnClickListener {
            shoppingCartManager.clearCart()
            Toast.makeText(this, "Carrinho limpo", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()

        // Para adicionar sombra e arredondar o fundo do diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.decorView?.elevation = 16f
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
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Limpar") { dialog, _ ->
                shoppingCartManager.updateObservation(item.product.id, "")
                dialog.dismiss()
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

    private fun formatCurrency(valueInCents: Double): String {
        val valueInReais = valueInCents / 10000.0
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(valueInReais)
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
                if (position < spanCount) outRect.top = spacing
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) outRect.top = spacing
            }
        }
    }

}