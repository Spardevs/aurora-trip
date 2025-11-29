package br.com.ticpass.pos.presentation.login.states

import br.com.ticpass.pos.domain.pos.model.Pos

sealed class LoginPosUiState {
    object Loading : LoginPosUiState()
    data class Success(val posList: List<Pos>) : LoginPosUiState()
    data class Error(val message: String) : LoginPosUiState()
    object Empty : LoginPosUiState()
}