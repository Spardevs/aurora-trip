package br.com.ticpass.pos.core.nfc.models

/**
 * PagSeguro supported nfc methods
 */
object SupportedNFCMethods {
    val methods = listOf(
        SystemNFCMethod.CUSTOMER_AUTH,
        SystemNFCMethod.TAG_FORMAT,
        SystemNFCMethod.CUSTOMER_SETUP,
        SystemNFCMethod.CART_READ,
        SystemNFCMethod.CART_UPDATE
    )
}