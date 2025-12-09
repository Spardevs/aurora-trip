package br.com.ticpass.pos.core.util

import br.com.ticpass.Constants
import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.local.entity.PosEntity

class CommisionUtils() {


    companion object {
        private lateinit var posDao: PosDao

        fun setPosDao(dao: PosDao) {
            posDao = dao
        }

        /**
         * Calculates the total amount including commission based on the provided value and the selected Pos.
         *
         * @param value The base value to calculate the commission on.
         * @return The total amount including the commission.
         */
        suspend fun calculateTotalWithCommission(value: Long): Double {
            val selectedPos: PosEntity = posDao.getSelectedPos(true) ?: return value.toDouble()
            val commissionAmount = value * (selectedPos.commission.toDouble() / Constants.CONVERSION_FACTOR)
            return value + commissionAmount
        }

    }
}