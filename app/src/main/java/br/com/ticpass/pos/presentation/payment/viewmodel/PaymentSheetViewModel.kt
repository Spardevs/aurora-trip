package br.com.ticpass.pos.presentation.payment.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.core.util.ShoppingCartUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class PaymentSheetViewModel @Inject constructor() : ViewModel() {

    private val _cartTotal = MutableLiveData<String>()
    val cartTotal: LiveData<String> = _cartTotal

    private val _cartVisible = MutableLiveData<Boolean>(false)
    val cartVisible: LiveData<Boolean> = _cartVisible

    fun updateCart(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val totalCents = ShoppingCartUtils.getTotalWithCommission(context)
            val totalFormatted = "R$ %.2f".format(totalCents / 100.0)
            _cartTotal.postValue(totalFormatted)
            _cartVisible.postValue(totalCents > 0)
        }
    }
}