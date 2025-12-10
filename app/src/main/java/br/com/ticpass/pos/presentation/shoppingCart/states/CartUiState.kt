package br.com.ticpass.pos.presentation.shoppingCart.states

import br.com.ticpass.pos.domain.shoppingCart.model.CartItemModel

data class CartUiState(
    val items: List<CartItemModel> = emptyList(),
    val totalQuantity: Int = 0,
    val totalWithoutCommission: Long = 0L,
    val totalWithCommission: Long = 0L,
    val isEmpty: Boolean = true
)