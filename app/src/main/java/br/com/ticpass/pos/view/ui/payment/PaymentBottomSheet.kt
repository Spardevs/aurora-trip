// PaymentBottomSheet.kt
package br.com.ticpass.pos.view.ui.payment

import PaymentAdapter
import PaymentMethod
import PaymentType
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class PaymentBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        dialog.behavior.isFitToContents = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_payment_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // configura cada cartão
        setupCard(view, R.id.cardPix,
            R.drawable.pix, "PIX", PaymentType.PIX)

        setupCard(view, R.id.cardCredit,
            R.drawable.credit, "Crédito", PaymentType.CREDIT_CARD)

        setupCard(view, R.id.cardDebit,
            R.drawable.debit, "Débito", PaymentType.DEBIT)

        setupCard(view, R.id.cardCash,
            R.drawable.cash, "Dinheiro", PaymentType.CASH)

        setupCard(view, R.id.cardVr,
            R.drawable.vr, "VR", PaymentType.VR)

        setupCard(view, R.id.splitPayment,
            R.drawable.vr, "Split", PaymentType.VR)
    }

    private fun setupCard(
        root: View,
        cardId: Int,
        @DrawableRes iconRes: Int,
        label: String,
        type: PaymentType
    ) {
        val card = root.findViewById<MaterialCardView>(cardId)
        val iv   = card.findViewById<ImageView>(R.id.ivIcon)
        val tv   = card.findViewById<TextView>(R.id.tvLabel)

        iv.setImageResource(iconRes)
        tv.text = label

        card.setOnClickListener {
            handleSelection(type)
        }
    }

    private fun handleSelection(type: PaymentType) {
        when (type) {
            PaymentType.PIX -> {
                Toast.makeText(requireContext(), "Pix selecionado", Toast.LENGTH_SHORT).show()
            }
            PaymentType.CREDIT_CARD -> {
                Toast.makeText(requireContext(), "Crédito selecionado", Toast.LENGTH_SHORT).show()
            }
            PaymentType.DEBIT -> {
                Toast.makeText(requireContext(), "Débito selecionado", Toast.LENGTH_SHORT).show()
            }
            PaymentType.CASH -> {
                Toast.makeText(requireContext(), "Dinheiro selecionado", Toast.LENGTH_SHORT).show()
            }
            PaymentType.VR -> {
                Toast.makeText(requireContext(), "VR selecionado", Toast.LENGTH_SHORT).show()
            }
        }
//        dismiss()
    }

    private fun openCreditCardDialog() {
        // inflar layout de cartão, etc…
        Toast.makeText(requireContext(), "Dialog Cartão de Crédito", Toast.LENGTH_SHORT).show()
    }
}

