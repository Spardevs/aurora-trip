package br.com.ticpass.pos.domain.shoppingCart.model

import br.com.ticpass.pos.domain.product.model.ProductModel

// CartItem holds a product and its quantity in the cart
data class CartItemModel(
    val product: ProductModel,
    val quantity: Int
)