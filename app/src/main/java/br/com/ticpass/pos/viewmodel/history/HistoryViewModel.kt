package br.com.ticpass.pos.viewmodel.history

import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.model.History
import br.com.ticpass.pos.data.room.entity.ProductEntity
import java.util.Date
import java.util.UUID
import br.com.ticpass.pos.R


class HistoryViewModel : ViewModel() {

    fun getHistory(): List<History> {
        return listOf(
            History(
                transactionId = "#6456123123",
                totalPrice = 1000.00,
                paymentPrice = 900.00,
                commissionPrice = 100.00,
                date = Date(),
                paymentMethod = "Cartão de crédito",
                description = "Venda aprovada com sucesso",
                products = listOf(
                    Pair(
                        ProductEntity(
                            id = UUID.randomUUID().toString(),
                            name = "Antártica Original 600ml",
                            thumbnail = R.drawable.ic_bitcoin_btc.toString(),
                            url = "https://example.com/product",
                            categoryId = "1",
                            price = 1200000,
                            stock = 10,
                            isEnabled = true
                        ),
                        1
                    ),
                    Pair(
                        ProductEntity(
                            id = UUID.randomUUID().toString(),
                            name = "Produto 2",
                            thumbnail= R.drawable.ic_bitcoin_btc.toString(),
                            url = "https://example.com/product2",
                            categoryId = "2",
                            price = 5000,
                            stock = 5,
                            isEnabled = true
                        ),
                        2
                    ),
                    Pair(
                        ProductEntity(
                            id = UUID.randomUUID().toString(),
                            name = "Produto 2",
                            thumbnail= R.drawable.ic_bitcoin_btc.toString(),
                            url = "https://example.com/product2",
                            categoryId = "2",
                            price = 5000,
                            stock = 5,
                            isEnabled = true
                        ),
                        2
                    )
                )
            )
        )
    }
}