package br.com.ticpass.pos.util

import java.math.BigInteger

fun calculatePercent(value: BigInteger): BigInteger {
    return (value / 100.toBigInteger())
}