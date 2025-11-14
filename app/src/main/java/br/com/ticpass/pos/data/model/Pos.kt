package br.com.ticpass.pos.data.model

import java.math.BigInteger

data class Pos(
    val id: String,
    val name: String,
    val commission: BigInteger?,
    val session: PosSessionUI?
) {
    data class PosSessionUI(
        val cashier: String?
    )
}