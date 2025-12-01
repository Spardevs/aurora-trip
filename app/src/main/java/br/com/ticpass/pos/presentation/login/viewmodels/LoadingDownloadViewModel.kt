package br.com.ticpass.pos.presentation.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.product.repository.ProductRepository
import br.com.ticpass.pos.domain.product.usecase.RefreshCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LoadingDownloadViewModel @Inject constructor(
    private val refreshCategoriesUseCase: RefreshCategoriesUseCase,
    private val refreshProductsUseCase: RefreshProductsUseCase,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingDownloadUiState>(LoadingDownloadUiState.Idle)
    val uiState: StateFlow<LoadingDownloadUiState> = _uiState

    fun startDownload(menuId: String) {
        viewModelScope.launch {
            _uiState.value = LoadingDownloadUiState.Loading("Baixando categorias...")
            try {
                refreshCategoriesUseCase(menuId)
                _uiState.value = LoadingDownloadUiState.Loading("Baixando produtos...")
                refreshProductsUseCase(menuId)
                _uiState.value = LoadingDownloadUiState.Loading("Baixando thumbnails...")
                val thumbnailsDir = File(/* contexto apropriado para diretório */)
                productRepository.downloadAndExtractThumbnails(menuId, thumbnailsDir)
                _uiState.value = LoadingDownloadUiState.Success("Download completo!")
            } catch (e: Exception) {
                _uiState.value = LoadingDownloadUiState.Error("Erro no download: ${e.message}")
            }
        }
    }
}

sealed class LoadingDownloadUiState {
    object Idle : LoadingDownloadUiState()
    data class Loading(val message: String) : LoadingDownloadUiState()
    data class Success(val message: String) : LoadingDownloadUiState()
    data class Error(val message: String) : LoadingDownloadUiState()
}