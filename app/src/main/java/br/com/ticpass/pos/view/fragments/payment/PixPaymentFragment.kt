package br.com.ticpass.pos.view.fragments.payment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.PaymentSelectionActivity
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.payment.PaymentState
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.utils.PixCodeGenerator
import br.com.ticpass.pos.payment.utils.PixOptions
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import com.google.zxing.EncodeHintType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PixPaymentFragment : Fragment() {
    private val paymentViewModel: PaymentProcessingViewModel by activityViewModels()

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    @Inject
    lateinit var pixCodeGenerator: PixCodeGenerator

    private lateinit var qrCodeImageView: ImageView
    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var priceTextView: TextView
    private lateinit var timerTextView: TextView


    private var paymentValue: Double = 0.0
    private var totalValue: Double = 0.0
    private var remainingValue: Double = 0.0
    private var isMultiPayment: Boolean = false
    private var progress: String = ""

    private var pixTimer: CountDownTimer? = null
    private val PIX_TIMEOUT_MS = 3 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obter valores dos arguments
        paymentValue = arguments?.getDouble("value_to_pay") ?: 0.0
        totalValue = arguments?.getDouble("total_value") ?: paymentValue
        remainingValue = arguments?.getDouble("remaining_value") ?: paymentValue
        isMultiPayment = arguments?.getBoolean("is_multi_payment") ?: false
        progress = arguments?.getString("progress") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_pix, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observePaymentState()

        if (::shoppingCartManager.isInitialized && ::pixCodeGenerator.isInitialized) {
            initViews(view)
            setupUI()
            generateAndDisplayPixQrCode()
            startPixTimer()
        } else {
            Log.e("PixPaymentFragment", "Dependencies not initialized")
            requireActivity().finish()
        }
    }

    private fun observePaymentState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.paymentState.collectLatest { state ->
                    handlePaymentState(state)
                }
            }
        }
    }

    private fun handlePaymentState(state: PaymentState) {
        when (state) {
            is PaymentState.Success -> {
                pixTimer?.cancel()
                statusTextView.text = "Pagamento Aprovado!"
                infoTextView.text = "Pagamento via PIX confirmado"
                timerTextView.text = "Concluído"

                requireActivity().setResult(AppCompatActivity.RESULT_OK)
                requireActivity().finish()
            }
            is PaymentState.Error -> {
                pixTimer?.cancel()
                statusTextView.text = "Erro no Pagamento"
                infoTextView.text = state.errorMessage
                timerTextView.text = "Erro"
            }
            else -> {
            }
        }
    }

    private fun initViews(view: View) {
        qrCodeImageView = view.findViewById(R.id.image)
        statusTextView = view.findViewById(R.id.payment_status)
        infoTextView = view.findViewById(R.id.payment_info)
        priceTextView = view.findViewById(R.id.payment_price)
        timerTextView = view.findViewById(R.id.timer_text)

        val cancelButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_pay)
        cancelButton?.setOnClickListener {
            cancelPixPayment()
            requireActivity().finish()
        }
    }

    private fun setupUI() {
        // Usar o valor específico deste pagamento (paymentValue)
        view?.findViewById<TextView>(R.id.payment_form)?.text = "PIX"
        statusTextView.text = "Aguardando pagamento via PIX"
        infoTextView.text = "Aponte a câmera do seu app bancário para o QR Code"
        timerTextView.text = "03:00"
        priceTextView.text = formatCurrency(paymentValue)

        // Mostrar progresso se for pagamento múltiplo
        if (isMultiPayment && progress.isNotEmpty()) {
            val progressTextView = view?.findViewById<TextView>(R.id.tv_progress)
            progressTextView?.visibility = View.VISIBLE
            progressTextView?.text = "Pagamento $progress"
        }
    }

    private fun generateAndDisplayPixQrCode() {
        val amount = (paymentValue * 100).toInt() // Converter para centavos

        val pixCode = pixCodeGenerator.generate(
            pixKey = "sua-chave-pix-aqui",
            amount = amount,
            options = PixOptions(
                description = "Pagamento TicPass",
                merchantName = "TicPass",
                merchantCity = "SAO PAULO",
                transactionId = "TXP${System.currentTimeMillis()}"
            )
        )

        Log.d("PixPaymentFragment", "Generated PIX code: $pixCode")

        generateQrCodeBitmap(pixCode)?.let { qrCodeBitmap ->
            qrCodeImageView.setImageBitmap(qrCodeBitmap)
            Log.d("PixPaymentFragment", "QR Code generated successfully")
        } ?: run {
            Log.e("PixPaymentFragment", "Failed to generate QR code")
            infoTextView.text = "Erro ao gerar QR Code. Tente novamente."
        }
    }

    private fun generateQrCodeBitmap(content: String, size: Int = 400): Bitmap? {
        return try {
            val hints = mapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e("PixPaymentFragment", "QR Code generation error: ${e.message}")
            null
        }
    }

    private fun startPixTimer() {
        pixTimer = object : CountDownTimer(PIX_TIMEOUT_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                timerTextView.text = timeFormatted
            }

            override fun onFinish() {
                expirePixPayment()
            }
        }.start()
    }

    private fun expirePixPayment() {
        statusTextView.text = "PIX Expirado"
        infoTextView.text = "O tempo para pagamento expirou. Gere um novo QR Code."
        timerTextView.text = "00:00"
        qrCodeImageView.alpha = 0.5f
        paymentViewModel.abortAllPayments()
        Log.d("PixPaymentFragment", "PIX payment expired due to timeout")
    }

    private fun cancelPixPayment() {
        pixTimer?.cancel()
        paymentViewModel.abortAllPayments()
        Log.d("PixPaymentFragment", "PIX payment cancelled by user")
    }

    override fun onResume() {
        super.onResume()
        if (::shoppingCartManager.isInitialized) {
            enqueuePayment()
        }
    }

    override fun onPause() {
        super.onPause()
        pixTimer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        pixTimer?.cancel()
        pixTimer = null
    }

    private fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }

    private fun enqueuePayment() {
        val amount = (paymentValue * 100).toInt()
        paymentViewModel.enqueuePayment(
            amount = amount,
            commission = 0,
            method = SystemPaymentMethod.PIX,
            isTransactionless = false
        )
    }

    private fun navigateBackToSelection() {
        val newRemainingValue = remainingValue - paymentValue

        if (newRemainingValue > 0) {
            val intent = Intent(requireContext(), PaymentSelectionActivity::class.java).apply {
                putExtra("total_value", totalValue)
                putExtra("remaining_value", newRemainingValue)
                putExtra("is_multi_payment", true)
                putExtra("progress", getNextProgress(progress))
            }
            startActivity(intent)
            requireActivity().finish()
        } else {
            shoppingCartManager.clearCart()
            requireActivity().finish()
        }
    }

    private fun getNextProgress(currentProgress: String): String {
        return try {
            val parts = currentProgress.split("/")
            if (parts.size == 2) {
                val current = parts[0].toInt()
                val total = parts[1].toInt()
                "${current + 1}/$total"
            } else {
                "2/?"
            }
        } catch (e: Exception) {
            "2/?"
        }
    }
}