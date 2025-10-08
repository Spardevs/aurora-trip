package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel

@AndroidEntryPoint
class PaymentErrorFragment : Fragment() {

    private var errorMessage: String? = null
    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            errorMessage = it.getString(ARG_ERROR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val paymentInfo = view.findViewById<TextView>(R.id.payment_info)
        val paymentErrorInfo = view.findViewById<TextView>(R.id.payment_error_info)
        val btnRetry = view.findViewById<MaterialButton>(R.id.btn_client_ticket) // Tentar Novamente
        val btnFinish = view.findViewById<MaterialButton>(R.id.btn_finish)

        paymentInfo?.text = "Pagamento Reprovado"
        paymentErrorInfo?.text = errorMessage ?: "Erro desconhecido"

        btnRetry?.setOnClickListener {
            // Sinaliza para o CardPaymentFragment re-tentar
            setFragmentResult("retry_payment", bundleOf("retry" to true))
            parentFragmentManager.popBackStack()
        }

        btnFinish?.setOnClickListener {
            paymentViewModel.abortAllPayments()
            requireActivity().finish()
        }
    }

    companion object {
        private const val ARG_ERROR = "error_message"

        @JvmStatic
        fun newInstance(error: String) =
            PaymentErrorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ERROR, error)
                }
            }
    }
}