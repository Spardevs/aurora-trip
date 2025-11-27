package br.com.ticpass.pos.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.menu.usecase.GetMenuItemsUseCase
import br.com.ticpass.pos.domain.menu.usecase.DownloadMenuLogoUseCase
import br.com.ticpass.pos.domain.menu.usecase.GetMenuLogoFileUseCase
import br.com.ticpass.pos.domain.menu.usecase.GetAllMenuLogoFilesUseCase
import br.com.ticpass.pos.presentation.login.states.LoginMenuUiState
import br.com.ticpass.pos.presentation.login.states.LogoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LoginMenuViewModel @Inject constructor(
    private val getMenuItemsUseCase: GetMenuItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginMenuUiState>(LoginMenuUiState.Loading)
    val uiState: StateFlow<LoginMenuUiState> = _uiState

    fun loadMenuItems(take: Int = 10, page: Int = 1) {
        viewModelScope.launch {
            getMenuItemsUseCase(take, page)
                .catch { exception ->
                    _uiState.value = LoginMenuUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { menus ->
                    _uiState.value = if (menus.isEmpty()) {
                        LoginMenuUiState.Empty
                    } else {
                        LoginMenuUiState.Success(menus)
                    }
                }
        }
    }
}

@HiltViewModel
class MenuLogoViewModel @Inject constructor(
    private val downloadMenuLogoUseCase: DownloadMenuLogoUseCase,
    private val getMenuLogoFileUseCase: GetMenuLogoFileUseCase,
    private val getAllMenuLogoFilesUseCase: GetAllMenuLogoFilesUseCase
) : ViewModel() {

    private val _logoUiState = MutableStateFlow<LogoUiState>(LogoUiState.Loading)
    val logoUiState: StateFlow<LogoUiState> = _logoUiState

    private val _localLogosState = MutableStateFlow<List<File>>(emptyList())
    val localLogosState: StateFlow<List<File>> = _localLogosState

    fun downloadLogo(logoId: String) {
        viewModelScope.launch {
            downloadMenuLogoUseCase(logoId)
                .catch { exception ->
                    _logoUiState.value = LogoUiState.Error(exception.message ?: "Failed to download logo")
                }
                .collect { file ->
                    if (file != null) {
                        _logoUiState.value = LogoUiState.Success(file)
                    } else {
                        _logoUiState.value = LogoUiState.Error("Failed to download logo")
                    }
                }
        }
    }

    fun getLocalLogo(logoId: String): File? {
        return getMenuLogoFileUseCase(logoId)
    }

    fun loadAllLocalLogos() {
        viewModelScope.launch {
            _localLogosState.value = getAllMenuLogoFilesUseCase()
        }
    }
}