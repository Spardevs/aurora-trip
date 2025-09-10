package br.com.ticpass.pos.viewmodel.history

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.model.History
import br.com.ticpass.pos.data.room.repository.HistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _histories = MutableLiveData<List<History>>()
    val histories: LiveData<List<History>> = _histories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadHistories() {
        viewModelScope.launch {
            val list = repository.getHistories()
            Log.d("HistoryViewModel", "Histories carregados: ${list.size}")
            _histories.postValue(list)
        }
    }
}