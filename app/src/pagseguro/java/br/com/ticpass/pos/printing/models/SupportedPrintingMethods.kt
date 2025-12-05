package br.com.ticpass.pos.core.printing.models

/**
 * PagSeguro supported printing methods
 */
object SupportedPrintingMethods {
    val methods = listOf(
        SystemPrintingMethod.ACQUIRER,
        SystemPrintingMethod.MP_4200_HS,
        SystemPrintingMethod.MPT_II,
    )
}