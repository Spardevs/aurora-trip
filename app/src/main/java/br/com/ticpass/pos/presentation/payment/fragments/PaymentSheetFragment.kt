package br.com.ticpass.pos.presentation.payment.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.core.util.ShoppingCartUtils
import br.com.ticpass.pos.databinding.FragmentPaymentSheetBinding
import br.com.ticpass.pos.presentation.product.viewmodels.ProductViewModel
import br.com.ticpass.pos.presentation.product.viewmodels.ProductViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PaymentSheetFragment : Fragment() {

    @Inject
    lateinit var productViewModelFactoryAssisted: ProductViewModel.Factory
    private var _binding: FragmentPaymentSheetBinding? = null
    private val binding get() = _binding!!
    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory(productViewModelFactoryAssisted, categoryId = null)
    }
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

        // Observar mudanças no carrinho
        updateCartDisplay()

        // Atualizar quando o carrinho mudar
        viewLifecycleOwner.lifecycleScope.launch {
            productViewModel.products.collect { _ ->
                updateCartDisplay()
            }
        }
    }

    fun updateCartDisplay() {
        val context = requireContext()
        val totalCents = ShoppingCartUtils.getTotalWithCommission(context)
        val totalFormatted = "R$ %.2f".format(totalCents / 100.0)

        binding.tvTotalPrice.text = totalFormatted

        // Mostrar/Esconder payment sheet baseado na quantidade de itens
        val hasItems = ShoppingCartUtils.getTotalQuantity(context) > 0
        binding.root.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}