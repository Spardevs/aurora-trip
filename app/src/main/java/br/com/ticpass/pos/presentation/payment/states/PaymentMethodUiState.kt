package br.com.ticpass.pos.presentation.payment.states

import androidx.annotation.DrawableRes

data class PaymentMethodUiState(
    val name: String,
    @DrawableRes val iconRes: Int
)