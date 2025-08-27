package br.com.ticpass.pos.view.fragments.payment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.PaymentEnqueuer

class CardPaymentFragment(
    private val paymentMethod: SystemPaymentMethod,
) : Fragment() {

    private var enqueuer: PaymentEnqueuer? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PaymentEnqueuer) {
            enqueuer = context
        } else {
            Toast.makeText(context, "Erro de configuração", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        view.post {
            enqueuePayment()
        }
    }

    private fun setupUI() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun enqueuePayment() {
        try {
            enqueuer?.let { enqueuer ->
                enqueuer.enqueuePayment(paymentMethod)
                showSuccessMessage()
            } ?: run {
                showErrorMessage()
                requireActivity().supportFragmentManager.popBackStack()
            }
        } catch (e: IllegalStateException) {
            Log.e("CardPaymentFragment", "Erro ao enfileirar pagamento: ${e.message}")
            showInitializationError()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun showSuccessMessage() {
        Toast.makeText(requireContext(), "Pagamento adicionado à fila", Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage() {
        Toast.makeText(requireContext(), "Erro ao processar pagamento", Toast.LENGTH_SHORT).show()
    }

    private fun showInitializationError() {
        Toast.makeText(requireContext(), "Sistema de pagamento não inicializado", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(paymentMethod: SystemPaymentMethod): CardPaymentFragment {
            return CardPaymentFragment(paymentMethod)
        }
    }
}