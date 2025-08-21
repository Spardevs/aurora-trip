package br.com.ticpass.pos.nfc.models

/**
 * Stone supported nfc methods
 */
object SupportedNFCMethods {
    val methods = listOf(
        SystemNFCMethod.CUSTOMER_AUTH,
        SystemNFCMethod.TAG_FORMAT,
        SystemNFCMethod.CUSTOMER_SETUP,
    )
}