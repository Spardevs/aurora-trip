// PixPaymentFragment.kt
package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.payment.models.SystemPaymentMethod

class PixPaymentFragment : Fragment() {

    private lateinit var paymentViewModel: PaymentProcessingViewModel

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_payment_pix, container, false)
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paymentViewModel = ViewModelProvider(requireActivity()).get(PaymentProcessingViewModel::class.java)
        setupUI()
        enqueuePayment()
    }

    private fun setupUI() {
        view?.findViewById<TextView>(R.id.payment_form)?.text = "PIX"
    }

    private fun enqueuePayment() {
        paymentViewModel.enqueuePayment(
            amount = 120000,
            commission = 0,
            method = SystemPaymentMethod.PIX,
            isTransactionless = false
        )
    }
}