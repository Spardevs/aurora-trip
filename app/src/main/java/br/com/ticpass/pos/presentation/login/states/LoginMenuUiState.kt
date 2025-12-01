package br.com.ticpass.pos.presentation.login.states

import br.com.ticpass.pos.domain.menu.model.Menu
import br.com.ticpass.pos.domain.menu.model.MenuDb
import java.io.File

sealed class LoginMenuUiState {
    object Loading : LoginMenuUiState()
    object Empty : LoginMenuUiState()
    data class Success(val menus: List<Menu>) : LoginMenuUiState()   // <-- domain model
    data class Error(val message: String) : LoginMenuUiState()
}

sealed class LogoUiState {
    object Loading : LogoUiState()
    data class Success(val file: File) : LogoUiState()
    data class Error(val message: String) : LogoUiState()
}