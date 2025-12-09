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

/**
 * Extension function to convert Int amount from cents to a larger unit
 * Uses MoneyUtils.fromCents
 * 
 * @return The converted amount as an Int
 */
fun Int.fromCents(): Int = MoneyUtils.fromCents(this)

/**
 * Extension function to convert Long amount from cents to a larger unit
 * Uses MoneyUtils.fromCents
 * 
 * @return The converted amount as an Int
 */
fun Long.fromCents(): Int = MoneyUtils.fromCents(this.toInt())

/**
 * Extension function to convert Int amount using the system's conversion factor
 * Uses MoneyUtils.fromConversionFactor
 *
 * @return The converted amount as an Int
 */
fun Int.toMoney(): Int = MoneyUtils.fromConversionFactor(this)

/**
 * Extension function to convert Long amount using the system's conversion factor
 * Uses MoneyUtils.fromConversionFactor
 *
 * @return The converted amount as an Int
 */
fun Long.toMoney(): Int = MoneyUtils.fromConversionFactor(this.toInt())

/**
 * Extension function to convert Int amount using the system's conversion factor
 * Uses MoneyUtils.fromConversionFactor
 *
 * @return The converted amount as an Int
 */
fun Int.toMoneyAsDouble(): Double = MoneyUtils.doubleFromConversionFactor(this)

/**
 * Extension function to convert Long amount using the system's conversion factor
 * Uses MoneyUtils.fromConversionFactor
 *
 * @return The converted amount as an Int
 */
fun Long.toMoneyAsDouble(): Double = MoneyUtils.doubleFromConversionFactor(this.toInt())
