package br.com.ticpass.pos.sdk

/**
 * Stone flavor capabilities.
 */
object FlavorCapabilities {
    
    val capabilities = AcquirerCapabilities(
        flavorName = "Stone",
        supportsPayment = true,
        supportsNFC = true,
        supportsPrinting = true,
        supportsRefund = true,
        requiresStoneCode = true,
        supportsLightningNetwork = true
    )
}
