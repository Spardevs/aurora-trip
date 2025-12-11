package br.com.ticpass.pos.presentation.payment.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.core.util.NumericConversionUtils
import br.com.ticpass.pos.databinding.FragmentPaymentSheetBinding
import br.com.ticpass.pos.presentation.payment.states.PaymentMethodUiState
import br.com.ticpass.pos.presentation.shoppingCart.activities.ShoppingCartActivity
import br.com.ticpass.pos.presentation.shoppingCart.states.CartUiState
import br.com.ticpass.pos.presentation.shoppingCart.viewmodels.CartViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PaymentSheetFragment : Fragment() {

    /**
     * Listener que a Activity hospedeira pode implementar
     * para receber a altura atual do payment sheet em pixels.
     */
    interface PaymentSheetHeightListener {
        fun onPaymentSheetHeightChanged(heightPx: Int)
    }

    companion object {
        private const val ARG_EXPANDED = "ARG_EXPANDED"

        fun newInstance(expanded: Boolean): PaymentSheetFragment {
            return PaymentSheetFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_EXPANDED, expanded)
                }
            }
        }
    }

    private var _binding: FragmentPaymentSheetBinding? = null
    private val binding get() = _binding!!

    private val numericConversionUtils = NumericConversionUtils
    private val cartViewModel: CartViewModel by activityViewModels()

    private val isExpanded: Boolean by lazy {
        arguments?.getBoolean(ARG_EXPANDED, false) ?: false
    }

    private var heightListener: PaymentSheetHeightListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Activity pode ou não implementar a interface
        heightListener = context as? PaymentSheetHeightListener
    }

    override fun onDetach() {
        super.onDetach()
        heightListener = null
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

        setupPaymentSheetClicks()
        setupPaymentMethods()
        setupListeners()
        observeCart()

        // Assim que o layout estiver pronto, calcula a altura inicial
        binding.root.doOnLayout {
            notifyCurrentHeight()
        }
    }

    private fun setupPaymentMethods() = binding.apply {
        val methods = listOf(
            PaymentMethodUiState("Crédito", R.drawable.ic_credit),
            PaymentMethodUiState("Débito", R.drawable.ic_debit),
            PaymentMethodUiState("PIX", R.drawable.ic_pix),
            PaymentMethodUiState("Dinheiro", R.drawable.ic_cash)
        )

        val container = paymentMethodsContainer
        container.removeAllViews()

        methods.forEach { method ->
            val itemView = layoutInflater.inflate(
                R.layout.item_payment_method,
                container,
                false
            )

            val iconView = itemView.findViewById<android.widget.ImageView>(R.id.iv_payment_icon)
            val nameView = itemView.findViewById<android.widget.TextView>(R.id.tv_payment_name)

            iconView.setImageResource(method.iconRes)
            nameView.text = method.name

            itemView.setOnClickListener {
                Timber.tag("PaymentSheet").d("Método selecionado: ${method.name}")
            }

            container.addView(itemView)
        }
    }

    private fun setupListeners() {
        if (!isExpanded) {
            binding.ivCart.setOnClickListener {
                val intent = Intent(requireContext(), ShoppingCartActivity::class.java)
                startActivity(intent)
            }
        } else {
            binding.ivCart.visibility = View.GONE
        }
    }

    private fun setupPaymentSheetClicks() = binding.apply {
        fun toggleForms() {
            if (paymentFormsContainer.visibility == View.VISIBLE) {
                paymentFormsContainer.visibility = View.GONE
            } else {
                paymentFormsContainer.visibility = View.VISIBLE
            }
            // sempre que abrir/fechar as formas, recalcula altura
            notifyCurrentHeight()
        }

        paymentSheetInfo.setOnClickListener { toggleForms() }
        paymentSheetInfoExpanded.setOnClickListener { toggleForms() }
    }

    private fun observeCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cartViewModel.uiState.collect { state ->
                    Timber.tag("PaymentSheet").d(
                        "uiState: isEmpty=${state.isEmpty}, " +
                                "qty=${state.totalQuantity}, " +
                                "totalWithout=${state.totalWithoutCommission}, " +
                                "totalWith=${state.totalWithCommission}"
                    )

                    if (state.isEmpty) {
                        hidePaymentSheet()
                    } else {
                        showPaymentSheet(state)
                    }
                }
            }
        }
    }

    private fun hidePaymentSheet() {
        binding.root.visibility = View.GONE
        heightListener?.onPaymentSheetHeightChanged(0)
    }

    private fun showPaymentSheet(state: CartUiState) {
        binding.root.visibility = View.VISIBLE

        if (isExpanded) {
            renderExpanded(state)
        } else {
            renderCompact(state)
        }

        notifyCurrentHeight()
    }

    private fun renderCompact(state: CartUiState) {
        binding.llPaymentSheet.visibility = View.VISIBLE
        binding.llPaymentSheetExpanded.visibility = View.GONE

        val totalFormatted =
            numericConversionUtils.convertLongToBrCurrencyString(state.totalWithCommission)
        val qty = state.totalQuantity
        val qtyText = if (qty == 1) "/ 1 item" else "/ $qty itens"

        binding.tvTotalPrice.text = totalFormatted
        binding.tvItemCount.text = qtyText
    }

    private fun renderExpanded(state: CartUiState) {
        binding.llPaymentSheet.visibility = View.GONE
        binding.llPaymentSheetExpanded.visibility = View.VISIBLE

        val subtotal = state.totalCommission
        val commission = state.totalWithoutCommission
        val total = state.totalWithCommission

        binding.txtSubtotal.text =
            numericConversionUtils.convertLongToBrCurrencyString(subtotal)

        binding.txtCommission.text =
            numericConversionUtils.convertLongToBrCurrencyString(commission)

        binding.txtTotal.text =
            numericConversionUtils.convertLongToBrCurrencyString(total)
    }

    /**
     * Calcula a altura efetiva atual do sheet (compacto/expandido + forms)
     * e avisa a Activity hospedeira.
     */
    private fun notifyCurrentHeight() {
        binding.root.post {
            if (_binding == null) return@post

            if (binding.root.visibility != View.VISIBLE) {
                heightListener?.onPaymentSheetHeightChanged(0)
                return@post
            }

            val mainHeight = when {
                binding.llPaymentSheet.visibility == View.VISIBLE ->
                    binding.llPaymentSheet.height
                binding.llPaymentSheetExpanded.visibility == View.VISIBLE ->
                    binding.llPaymentSheetExpanded.height
                else -> 0
            }

            val formsHeight =
                if (binding.paymentFormsContainer.visibility == View.VISIBLE)
                    binding.paymentFormsContainer.height
                else
                    0

            val totalHeight = mainHeight + formsHeight
            heightListener?.onPaymentSheetHeightChanged(totalHeight)
        }
    }

    override fun onResume() {
        super.onResume()
        cartViewModel.reloadCartFromPrefs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}