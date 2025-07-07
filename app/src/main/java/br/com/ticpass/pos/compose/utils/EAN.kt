package br.com.ticpass.pos.compose.utils

import kotlin.random.Random

fun calculateEAN13CheckDigit(code: String): String {
    require(code.length == 12) { "EAN-13 code must be 12 digits long" }

    val sum = code.mapIndexed { index, char ->
        val digit = char.toString().toInt()
        if (index % 2 == 0) digit else digit * 3
    }.sum()

    val checkDigit = (10 - (sum % 10)) % 10
    return code + checkDigit
}

fun generateRandomEAN(): String {
    var code = ""
    for (i in 1..12) {
        code += Random.nextInt(0, 9).toString()
    }
    return calculateEAN13CheckDigit(code)
}