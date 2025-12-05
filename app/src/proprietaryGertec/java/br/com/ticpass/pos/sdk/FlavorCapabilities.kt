package br.com.ticpass.pos.sdk

/**
 * ProprietaryGertec flavor capabilities.
 * This flavor only supports printing - payment, NFC, and refund are NO-OP.
 */
object FlavorCapabilities {
    
    val capabilities = AcquirerCapabilities(
        flavorName = "ProprietaryGertec",
        supportsPayment = false,
        supportsNFC = false,
        supportsPrinting = true,
        supportsRefund = false,
        requiresStoneCode = false,
        supportsLightningNetwork = false
    )
}
