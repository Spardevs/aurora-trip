package br.com.ticpass.pos.core.sdk

/**
 * Defines the capabilities of an acquirer flavor.
 * Used for runtime feature detection instead of compile-time assumptions.
 */
data class AcquirerCapabilities(
    /**
     * Human-readable name of the flavor (e.g., "PagSeguro", "Stone", "ProprietaryGertec")
     */
    val flavorName: String,
    
    /**
     * Whether this flavor supports payment processing
     */
    val supportsPayment: Boolean,
    
    /**
     * Whether this flavor supports NFC operations
     */
    val supportsNFC: Boolean,
    
    /**
     * Whether this flavor supports printing
     */
    val supportsPrinting: Boolean,
    
    /**
     * Whether this flavor supports refund operations
     */
    val supportsRefund: Boolean,
    
    /**
     * Whether this flavor requires a Stone code for initialization (Stone-specific)
     */
    val requiresStoneCode: Boolean = false,
    
    /**
     * Whether this flavor supports Lightning Network Bitcoin payments (Stone-specific)
     */
    val supportsLightningNetwork: Boolean = false
)
