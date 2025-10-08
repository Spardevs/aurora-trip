package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import javax.inject.Inject
import androidx.appcompat.app.AppCompatActivity

@AndroidEntryPoint
class PaymentSuccessFragment : Fragment() {

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    private var isMultiPayment: Boolean = false
    private var progress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isMultiPayment = it.getBoolean(ARG_MULTI, false)
            progress = it.getString(ARG_PROGRESS, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_success, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnFinish = view.findViewById<MaterialButton>(R.id.btn_finish)

        btnFinish?.setOnClickListener {
            if (isMultiPayment) {
                requireActivity().setResult(AppCompatActivity.RESULT_OK)
                requireActivity().finish()
            } else {
                // limpa o carrinho e finaliza
                shoppingCartManager.clearCart()
                requireActivity().finish()
            }
        }
    }

    companion object {
        private const val ARG_MULTI = "is_multi_payment"
        private const val ARG_PROGRESS = "progress"

        @JvmStatic
        fun newInstance(isMultiPayment: Boolean, progress: String) =
            PaymentSuccessFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MULTI, isMultiPayment)
                    putString(ARG_PROGRESS, progress)
                }
            }
    }
}