// imports principais (exemplo)
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.data.activity.BarcodeScannerActivity
import br.com.ticpass.pos.viewmodel.refund.PaymentRefundViewModel
import br.com.ticpass.pos.feature.refund.RefundViewModel
import br.com.ticpass.pos.queue.processors.refund.processors.models.RefundProcessorType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RefundFragment : Fragment() {

    private val paymentRefundViewModel: PaymentRefundViewModel by viewModels()
    private val refundViewModel: RefundViewModel by viewModels() // ou activityViewModels() se for compartilhado

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val scannedText = data?.getStringExtra(BarcodeScannerActivity.EXTRA_SCAN_TEXT) ?: return@registerForActivityResult

            // Pede ao PaymentRefundViewModel que carregue o pagamento (resolve token -> paymentId -> DB)
            paymentRefundViewModel.loadPaymentFromScanned(scannedText)

            // Observa o resultado e, se achar, enfileira o refund
            lifecycleScope.launch {
                paymentRefundViewModel.uiState.collectLatest { state ->
                    when (state) {
                        is PaymentRefundViewModel.UiState.Loading -> {
                            // show loading
                        }
                        is PaymentRefundViewModel.UiState.Success -> {
                            val payment = state.payment
                            val atk = payment.acquirerTransactionKey
                            val txId = payment.transactionId

                            if (atk.isNotBlank() && txId.isNotBlank()) {
                                // Enfileira o refund e inicia processamento
                                refundViewModel.enqueueRefund(
                                    atk = atk,
                                    txId = txId,
                                    isQRCode = false,
                                    processorType = RefundProcessorType.ACQUIRER // ajuste se enum tiver outro nome
                                )
                                refundViewModel.startProcessing()
                            } else {
                                // tratar caso faltando ATK/TX
                                // mostrar erro ao usuário
                            }
                        }
                        is PaymentRefundViewModel.UiState.Error -> {
                            // mostrar state.message para o usuário
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // Em algum lugar (botão "Escanear" por exemplo) chame:
    private fun openScanner() {
        val intent = BarcodeScannerActivity.createIntent(requireContext())
        scanLauncher.launch(intent)
    }
}