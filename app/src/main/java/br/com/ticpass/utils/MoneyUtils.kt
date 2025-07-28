/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

package br.com.ticpass.utils

import br.com.ticpass.Constants

/**
 * Utility class for monetary value operations
 */
object MoneyUtils {

    /**
     * Converts monetary value from smaller units (e.g., cents) to a larger unit (e.g., dollars)
     * 
     * @param amount The amount in smaller units (e.g., cents)
     * @param conversionFactor The factor to divide by (default is from Constants.CONVERSION_FACTOR)
     * @return Converted amount as an integer
     */
    private fun convertAmount(
        amount: Int,
        conversionFactor: Long = Constants.CONVERSION_FACTOR
    ): Int {
        return (amount / conversionFactor).toInt()
    }

    /**
     * Converts monetary value from cents
     * This is a convenience method that uses a conversion factor of 100
     *
     * @param amount The amount in cents
     * @return Converted amount as an integer
     */
    fun fromCents(amount: Int): Int {
        return convertAmount(amount, 100L)
    }

    /**
     * Converts monetary value from system CONVERSION_FACTOR
     * This is a convenience method that uses the system's conversion factor constant
     *
     * @param amount The amount in cents
     * @return Converted amount as an integer
     */
    fun fromConversionFactor(amount: Int): Int {
        return convertAmount(amount)
    }
}
