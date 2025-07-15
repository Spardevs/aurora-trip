package br.com.ticpass.pos.sdk.nfc

/**
 * Singleton holder for BaseNFCProvider implementation
 * 
 * This pattern allows different flavors to provide their own implementation
 * while maintaining the same interface and access pattern.
 */
object NFCProvider {
    /**
     * The singleton instance of the NFC provider
     * Each flavor will implement this differently in their source set
     */
    val instance: BaseNFCProvider by lazy {
        // Each flavor would replace this implementation in their source set
        // For base module, we'll throw an exception if it's not overridden
        throw NotImplementedError("BaseNFCProvider implementation not found. Make sure a flavor is selected.")
    }
}
