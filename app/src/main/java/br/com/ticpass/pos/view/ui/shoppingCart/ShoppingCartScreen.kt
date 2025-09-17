package br.com.ticpass.pos.view.ui.shoppingCart

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.activity.PaymentActivity
import br.com.ticpass.pos.data.activity.SplitEqualActivity
import br.com.ticpass.pos.data.activity.SplitManualActivity
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
class ShoppingCartScreen : BaseActivity() {
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
        PaymentMethod("VR", R.drawable.vr, "vr"),
        PaymentMethod("Pix", R.drawable.pix,  "pix")
    )
    private fun setupPaymentMethods() {
        val container = paymentSheet.findViewById<GridLayout>(R.id.payment_methods_container)
        container.removeAllViews()

        paymentMethods.forEachIndexed { index, method ->
            val itemView = LayoutInflater.from(this)
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
                val intent = Intent(this, PaymentScreen::class.java)
                intent.putExtra("payment_type", method.value)
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
                    // Implemente a lógica para lidar com a divisão
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

            paymentSheet.findViewById<ImageView>(R.id.btnOptions)?.let { btnOptions ->
                btnOptions.setOnClickListener {
                    showSplitBillDialog()
                }
            }
        }
    }

    private fun updatePaymentInfo(cart: ShoppingCartManager.ShoppingCart) {

        paymentSheet.findViewById<TextView>(R.id.tv_total_price)?.text =
            formatCurrency(cart.totalPrice.toDouble())
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

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
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