package br.com.ticpass.pos.presentation.scanners.states

sealed class QrLoginState {
    object Idle : QrLoginState()
    object Processing : QrLoginState()
    data class Success(val token: String) : QrLoginState()
    data class Error(val message: String) : QrLoginState()
}