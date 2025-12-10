package br.com.ticpass.pos.presentation.payment.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.core.util.NumericConversionUtils
import br.com.ticpass.pos.databinding.FragmentPaymentSheetBinding
import br.com.ticpass.pos.presentation.shoppingCart.viewmodels.CartViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentSheetFragment : Fragment() {

    private var _binding: FragmentPaymentSheetBinding? = null
    private val binding get() = _binding!!

    private val numericConversionUtils = NumericConversionUtils

    private val cartViewModel: CartViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cartViewModel.uiState.collect { state ->
                    android.util.Log.d("PaymentSheet", "uiState: isEmpty=${state.isEmpty}, qty=${state.totalQuantity}, total=${state.totalWithCommission}")

                    if (state.isEmpty) {
                        binding.root.visibility = View.GONE
                    } else {
                        binding.root.visibility = View.VISIBLE
                        binding.tvTotalPrice.text =
                            numericConversionUtils.convertLongToBrCurrencyString(state.totalWithCommission)
                        val qty = state.totalQuantity
                        binding.tvItemCount.text =
                            if (qty == 1) "/ 1 item" else "/ $qty itens"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}