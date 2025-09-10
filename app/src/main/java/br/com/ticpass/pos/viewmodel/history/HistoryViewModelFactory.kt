package br.com.ticpass.pos.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.ticpass.pos.data.room.repository.HistoryRepository

class HistoryViewModelFactory(
    private val repository: HistoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel class desconhecido")
    }
}