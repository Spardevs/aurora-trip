package br.com.ticpass.pos.viewmodel.refund

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.room.entity.PaymentEntity
import br.com.ticpass.pos.data.room.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PaymentRefundViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val payment: PaymentEntity) : UiState()
        data class Error(val message: String?) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _payment = MutableStateFlow<PaymentEntity?>(null)
    val payment: StateFlow<PaymentEntity?> = _payment.asStateFlow()

    /**
     * Resolve um texto lido pelo scanner para um paymentId que exista no banco.
     * - Remove sufixos como "-1" ou "-G"
     * - Se for EAN13 (13 dígitos numéricos), tenta lookup em SharedPreferences ("BarcodeMappingPrefs", key "map_<ean13>")
     * - Retorna o paymentId resolvido (ou o candidato original se lookup não existir)
     */
    private fun resolveScannedToPaymentId(scanned: String): String {
        val base = scanned.split("|").map { it.trim() }.firstOrNull() ?: scanned.trim()
        val candidate = base.split("-").firstOrNull()?.trim() ?: base
        val numeric = candidate.replace("[^0-9]".toRegex(), "")

        val prefs = try {
            appContext.getSharedPreferences("BarcodeMappingPrefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }

        if (numeric.length == 13) {
            val mapped = prefs?.getString("map_$numeric", null)
            return mapped?.takeIf { it.isNotBlank() } ?: numeric
        }

        if (numeric.length == 12) {
            val withLeadingZero = "0$numeric"
            val mapped = prefs?.getString("map_$withLeadingZero", null)
            if (!mapped.isNullOrBlank()) return mapped
            return numeric
        }

        return candidate
    }

    /**
     * Busca o pagamento pelo texto lido (scannedPayload).
     * Atualiza uiState e o payment StateFlow.
     */
    fun loadPaymentFromScanned(scannedPayload: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val paymentId = resolveScannedToPaymentId(scannedPayload)
                Timber.tag("LoadPaymentFromScanned").d("Resolved paymentId -> $paymentId; original payload: $scannedPayload")
                val p = paymentRepository.getById(paymentId)
                if (p != null) {
                    _payment.value = p
                    _uiState.value = UiState.Success(p)
                } else {
                    _payment.value = null
                    _uiState.value = UiState.Error("Pagamento não encontrado para id: $paymentId")
                }
            } catch (e: Exception) {
                _payment.value = null
                _uiState.value = UiState.Error(e.message)
            }
        }
    }

    /**
     * Consulta direta (suspending) - útil se preferir chamar do lifecycleScope do fragment/activity.
     */
    suspend fun getPaymentDirect(paymentId: String): PaymentEntity? {
        return try {
            paymentRepository.getById(paymentId)
        } catch (e: Exception) {
            null
        }
    }
}