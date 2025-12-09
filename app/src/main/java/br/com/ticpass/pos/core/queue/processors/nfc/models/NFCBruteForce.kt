package br.com.ticpass.pos.core.queue.processors.nfc.models

enum class NFCBruteForce {
    /**
     * All owned keys and all known keys will be used, including brute force attempts.
     */
    FULL,

    /**
     * Only owned keys and list of known (and likely to succeed) keys will be used.
     */
    MOST_LIKELY,

    /**
     * Only owned keys will be used, no brute force attempt will be made.
     */
    NONE
}