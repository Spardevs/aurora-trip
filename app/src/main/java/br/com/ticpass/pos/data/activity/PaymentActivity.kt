package br.com.ticpass.pos.data.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.PaymentSheetBinding
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.viewmodel.payment.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PaymentActivity : Fragment() {

    private lateinit var viewModel: PaymentViewModel
    private lateinit var binding: PaymentSheetBinding

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PaymentSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext().getSharedPreferences("ShoppingCartPrefs", Context.MODE_PRIVATE)
        viewModel = ViewModelProvider(this, PaymentViewModelFactory(sharedPrefs, shoppingCartManager))[PaymentViewModel::class.java]

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        binding.rvPaymentMethods.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun setupObservers() {
        shoppingCartManager.cartUpdates.observe(viewLifecycleOwner) {
            Log.d("PaymentActivity", "CartUpdates observed - Updating UI")
            updateCartUI()
        }
    }

    private fun updateCartUI() {
        val cart = shoppingCartManager.getCart()
        Log.d("PaymentActivity", "Updating UI - Items: ${cart.items.values.sum()}, Total: ${cart.totalPrice}")

        binding.tvItemsCount.text = resources.getQuantityString(
            R.plurals.items_count,
            cart.items.values.sum(),
            cart.items.values.sum()
        )
        binding.tvTotalPrice.text = formatCurrency(cart.totalPrice.toDouble())
        binding.btnClearAll.visibility = if (cart.items.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }
}
class PaymentViewModelFactory(
    private val sharedPrefs: SharedPreferences,
    private val shoppingCartManager: ShoppingCartManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(sharedPrefs, shoppingCartManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}