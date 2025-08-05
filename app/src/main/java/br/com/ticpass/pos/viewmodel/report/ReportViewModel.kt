package br.com.ticpass.pos.viewmodel.report

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.util.saveReportAsBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState

    fun generateReport(context: Context) {
        _uiState.value = ReportUiState(isLoading = true, statusMessage = "Generating report...")
        viewModelScope.launch {
            try {
                val reportData = ReportData(
                    eventTitle = "TICPASS",
                    eventDate = "31/07/2025 - 31/07/2025",
                    totalAmount = "R$2,20",
                    cashAmount = "R$2,20",
                    bitcoinAmount = "R$0,00",
                    debitAmount = "R$0,00",
                    creditAmount = "R$0,00",
                    pixAmount = "R$0,00",
                    mealVoucherAmount = "R$0,00",
                    totalInAmount = "R$2,20",
                    refundAmount = "R$0,00",
                    withdrawalAmount = "R$0,00",
                    totalOutAmount = "- R$0,00",
                    productDescription = "4x CRÉDITO À VISTA N",
                    productUnitPrice = "R$0,55",
                    productTotal = "R$2,20",
                    serialNumber = "EMULATOR35X6X11X0",
                    cashierName = "CAIXA-003",
                    operatorName = "teteteaTeste",
                    commissionAmount = "R$0,20",
                    openingTime = "31/07/2025 12:31",
                    reprintedTickets = "0",
                    reprintedAmount = "R$0,00"
                )

                val bitmapFile = saveReportAsBitmap(context, reportData)

                _uiState.value = ReportUiState(
                    isLoading = false,
                    statusMessage = "Report generated successfully",
                    previewBitmap = bitmapFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
                )
            } catch (e: Exception) {
                _uiState.value = ReportUiState(
                    isLoading = false,
                    statusMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun shareReport(context: Context) {
        val bitmap = _uiState.value.previewBitmap ?: return
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, bitmap)
            type = "image/png"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }
}

data class ReportUiState(
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val previewBitmap: Bitmap? = null
)


data class ReportData(
    val eventTitle: String,
    val eventDate: String,
    val totalAmount: String,
    val cashAmount: String,
    val bitcoinAmount: String,
    val debitAmount: String,
    val creditAmount: String,
    val pixAmount: String,
    val mealVoucherAmount: String,
    val totalInAmount: String,
    val refundAmount: String,
    val withdrawalAmount: String,
    val totalOutAmount: String,
    val productDescription: String,
    val productUnitPrice: String,
    val productTotal: String,
    val serialNumber: String,
    val cashierName: String,
    val operatorName: String,
    val commissionAmount: String,
    val openingTime: String,
    val reprintedTickets: String,
    val reprintedAmount: String
)