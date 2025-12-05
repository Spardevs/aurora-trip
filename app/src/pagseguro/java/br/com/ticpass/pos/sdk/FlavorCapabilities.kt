package br.com.ticpass.pos.sdk

/**
 * PagSeguro flavor capabilities.
 */
object FlavorCapabilities {
    
    val capabilities = AcquirerCapabilities(
        flavorName = "PagSeguro",
        supportsPayment = true,
        supportsNFC = true,
        supportsPrinting = true,
        supportsRefund = true,
        requiresStoneCode = false,
        supportsLightningNetwork = false
    )
}
