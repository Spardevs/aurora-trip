package br.com.ticpass.pos.core.nfc.models

/**
 * Enum representing balance operations on NFC tags.
 * Balance is stored as 3 bytes (0-16,777,215 cents = ~$167,772.15 max).
 */
enum class BalanceOperation {
    /**
     * Set the balance to a specific amount.
     * Replaces any existing balance value.
     */
    SET,
    
    /**
     * Clear the balance (set to 0).
     * Equivalent to SET with amount = 0.
     */
    CLEAR
}
