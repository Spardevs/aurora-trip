package br.com.ticpass.pos.presentation.payment.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.core.util.NumericConversionUtils
import br.com.ticpass.pos.databinding.FragmentPaymentSheetBinding
import br.com.ticpass.pos.presentation.shoppingCart.viewmodels.CartViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PaymentSheetFragment : Fragment() {

    private var _binding: FragmentPaymentSheetBinding? = null
    private val numericConversionUtils = NumericConversionUtils

    private val binding get() = _binding!!

    // Compartilha o ViewModel da tela de produtos para observar o carrinho
    private val cartViewModel: CartViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            cartViewModel.cartItems.collect { cartItems ->
                if (cartItems.isEmpty()) {
                    binding.root.visibility = View.GONE
                } else {
                    binding.root.visibility = View.VISIBLE
                    val totalPrice = cartItems.sumOf { it.product.price * it.quantity }
                    binding.tvTotalPrice.text = numericConversionUtils.convertLongToBrCurrencyString(totalPrice)
                    binding.tvItemCount.text = "${cartItems.sumOf { it.quantity }} itens no carrinho"
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}