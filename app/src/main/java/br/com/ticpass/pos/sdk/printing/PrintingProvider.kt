package br.com.ticpass.pos.sdk.printing

/**
 * Singleton holder for BasePrintingProvider implementation
 * 
 * This pattern allows different flavors to provide their own implementation
 * while maintaining the same interface and access pattern.
 */
object PrintingProvider {
    /**
     * The singleton instance of the printing provider
     * Each flavor will implement this differently in their source set
     */
    val instance: BasePrintingProvider by lazy {
        // Each flavor would replace this implementation in their source set
        // For base module, we'll throw an exception if it's not overridden
        throw NotImplementedError("BasePrintingProvider implementation not found. Make sure a flavor is selected.")
    }
}
