package br.com.ticpass.pos.presentation.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.pos.usecase.GetPosByMenuUseCase
import br.com.ticpass.pos.domain.pos.usecase.RefreshPosListUseCase
import br.com.ticpass.pos.presentation.login.states.LoginPosUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginPosViewModel @Inject constructor(
    private val getPosByMenuUseCase: GetPosByMenuUseCase,
    private val refreshPosListUseCase: RefreshPosListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginPosUiState>(LoginPosUiState.Loading)
    val uiState: StateFlow<LoginPosUiState> = _uiState

    // Track pagination state
    private var currentPage = 1
    private var hasNextPage = true
    private var isLoading = false

    /**
     * Load POS list with pagination support
     */
    fun loadPosList(menuId: String, authorization: String, cookie: String, page: Int = 1) {
        // Prevent multiple simultaneous requests
        if (isLoading) return

        isLoading = true
        viewModelScope.launch {
            // Show loading state only when loading first page
            if (page == 1) {
                _uiState.value = LoginPosUiState.Loading
            }

            val result = refreshPosListUseCase(10, page, menuId, "both")
            result.onSuccess { posList ->
                // Update pagination info
                // Note: We need to get pageInfo from the response to properly handle pagination
                // For now, we'll assume there's no more pages after the first request
                // In a real implementation, we would get this info from the API response
                hasNextPage = false // This should come from the API response

                if (page == 1) {
                    _uiState.value = LoginPosUiState.Success(posList)
                } else {
                    // Append to existing list
                    val currentList = (_uiState.value as? LoginPosUiState.Success)?.posList ?: emptyList()
                    _uiState.value = LoginPosUiState.Success(currentList + posList)
                }

                isLoading = false
            }.onFailure { ex ->
                isLoading = false
                if (page == 1) {
                    // Try to show cached data if available
                    getPosByMenuUseCase(menuId)
                        .catch { _uiState.value = LoginPosUiState.Error(ex.message ?: "Falha ao carregar") }
                        .collectLatest { cached ->
                            if (cached.isEmpty()) {
                                _uiState.value = LoginPosUiState.Error(ex.message ?: "Falha ao carregar")
                            } else {
                                _uiState.value = LoginPosUiState.Success(cached)
                            }
                        }
                } else {
                    // For subsequent pages, just show error without changing the current list
                    // The UI can show a retry button or similar
                }
            }
        }
    }

    /**
     * Load next page if available
     */
    fun loadNextPage(menuId: String, authorization: String, cookie: String) {
        if (hasNextPage && !isLoading) {
            loadPosList(menuId, authorization, cookie, currentPage + 1)
        }
    }

    /**
     * Reset pagination and load first page
     */
    fun refresh(menuId: String, authorization: String, cookie: String) {
        currentPage = 1
        hasNextPage = true
        loadPosList(menuId, authorization, cookie, 1)
    }

    @Deprecated("Use loadPosList instead", ReplaceWith("loadPosList(menuId, authorization, cookie, 1)"))
    fun observeMenu(menuId: String, authorization: String, cookie: String) {
        refresh(menuId, authorization, cookie)
    }
}