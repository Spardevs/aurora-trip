package br.com.ticpass.pos.core.util

import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.local.entity.PosEntity

class CommisionUtils(private val posDao: PosDao) {

    /**
     * Calculates the total amount including commission based on the provided value and the selected Pos.
     *
     * @param value The base value to calculate the commission on.
     * @return The total amount including the commission.
     */
    suspend fun calculateTotalWithCommission(value: Double): Double {
        // Retrieve the selected PosEntity
        val selectedPos: PosEntity = posDao.getSelectedPos(true) ?: return value

        // Calculate the commission amount
        val commissionAmount = value * (selectedPos.commission.toDouble() / 100)

        // Return the total amount including commission
        return value + commissionAmount
    }
}