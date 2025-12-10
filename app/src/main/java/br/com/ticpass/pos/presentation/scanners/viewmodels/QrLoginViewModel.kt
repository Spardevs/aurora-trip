package br.com.ticpass.pos.presentation.scanners.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.presentation.scanners.states.QrLoginState
import br.com.ticpass.pos.domain.login.usecase.SignInWithQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class QrLoginViewModel @javax.inject.Inject constructor(
    private val signInWithQrUseCase: SignInWithQrUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<QrLoginState>(QrLoginState.Idle)
    val state: StateFlow<QrLoginState> = _state.asStateFlow()

    fun signInWithQr(qrText: String) {
        viewModelScope.launch {
            _state.value = QrLoginState.Processing
            try {
                val parts = qrText.split("@")
                if (parts.size != 2) {
                    _state.value = QrLoginState.Error("Formato inválido. Esperado: token@pin")
                    return@launch
                }

                val shortLivedToken = parts[0].trim()

                val result = signInWithQrUseCase(shortLivedToken)

                if (result.isSuccess) {
                    val (_, tokens) = result.getOrNull()!!
                    val accessToken = tokens.first
                    _state.value = QrLoginState.Success(accessToken ?: "")
                } else {
                    _state.value = QrLoginState.Error(result.exceptionOrNull()?.message ?: "Erro desconhecido")
                }
            } catch (e: Exception) {
                _state.value = QrLoginState.Error(e.message ?: "Erro inesperado")
            }
        }
    }

    fun resetToIdle() {
        _state.value = QrLoginState.Idle
    }
}