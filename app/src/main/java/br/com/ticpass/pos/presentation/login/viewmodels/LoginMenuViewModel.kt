package br.com.ticpass.pos.presentation.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.menu.repository.MenuRepository
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
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class LoginMenuViewModel @Inject constructor(
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
    private val downloadMenuLogoUseCase: DownloadMenuLogoUseCase,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginMenuUiState>(LoginMenuUiState.Loading)
    val uiState: StateFlow<LoginMenuUiState> = _uiState

    fun loadMenuItems(take: Int = 10, page: Int = 1) {
        Timber.d("LoginMenuViewModel.loadMenuItems called take=$take page=$page")
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

    /**
     * Baixa a logo de forma lazy e atualiza o caminho local no banco para o menu informado.
     * rawLogo pode ser id ou URL (será normalizado automaticamente).
     */
    fun downloadLogoForMenu(menuId: String, rawLogo: String?) {
        Timber.d("downloadLogoForMenu called menuId=$menuId rawLogo=$rawLogo")
        if (rawLogo.isNullOrBlank()) return

        viewModelScope.launch {
            val logoParam = try {
                if (rawLogo.startsWith("http")) rawLogo.toUri().lastPathSegment ?: rawLogo
                else rawLogo
            } catch (e: Exception) {
                rawLogo
            }

            Timber.d("downloadLogoForMenu -> normalized logoParam=$logoParam for menuId=$menuId")

            downloadMenuLogoUseCase(logoParam)
                .catch { e ->
                    Timber.e(e, "downloadLogoForMenu failed for $logoParam")
                }
                .collect { file ->
                    Timber.d("downloadLogoForMenu collect file=$file for menuId=$menuId")
                    file?.let {
                        try {
                            // atualiza DB para que o fluxo do Room emita novamente
                            menuRepository.updateMenuLogoPath(menuId, it.absolutePath)
                            Timber.d("Logo baixada e DB atualizada: menuId=$menuId path=${it.absolutePath}")
                        } catch (e: Exception) {
                            Timber.e(e, "Erro ao atualizar DB com logo para menuId=$menuId")
                        }
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