package br.com.ticpass.pos.viewmodel.withdrawal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WithdrawalViewModel @Inject constructor() : ViewModel() {

    private val _availableBalance = MutableLiveData<Double>()
    val availableBalance: LiveData<Double> = _availableBalance

    private val _totalWithdrawn = MutableLiveData<Double>()
    val totalWithdrawn: LiveData<Double> = _totalWithdrawn

    private val _withdrawalHistory = MutableLiveData<List<Withdrawal>>()
    val withdrawalHistory: LiveData<List<Withdrawal>> = _withdrawalHistory

    init {
        _withdrawalHistory.value = emptyList()
        _totalWithdrawn.value = 0.0
    }

    fun initializeBalance(initialBalance: Double) {
        _availableBalance.value = initialBalance
    }

    fun addWithdrawal(amount: Double) {
        val currentBalance = _availableBalance.value ?: 0.0
        val currentTotal = _totalWithdrawn.value ?: 0.0
        val currentHistory = _withdrawalHistory.value ?: emptyList()

        if (amount <= currentBalance) {
            _availableBalance.value = currentBalance - amount
            _totalWithdrawn.value = currentTotal + amount
            _withdrawalHistory.value = currentHistory + Withdrawal(
                amount = amount,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

data class Withdrawal(
    val amount: Double,
    val timestamp: Long
)