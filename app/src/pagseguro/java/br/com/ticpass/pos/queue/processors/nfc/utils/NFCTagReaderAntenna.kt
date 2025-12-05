package br.com.ticpass.pos.core.queue.processors.nfc.utils

import br.com.ticpass.pos.core.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException

/**
 * Utility for NFC reader antenna operations.
 */
object NFCTagReaderAntenna {
    private const val TAG = "NFCTagReaderAntenna"
    private val plugpag = AcquirerSdk.nfc.getInstance()

    /**
     * Starts the NFC reader antenna.
     */
    fun start(): Boolean {
        return try {
            val start = plugpag.startNFCCardDirectly()

            if (start != PlugPag.NFC_RET_OK) {
                stop()
                false
            } else
                true
        } catch (e: PlugPagException) {
            stop()
            false
        }
    }

    /**
     * Stops the NFC reader antenna.
     * Returns true if successful, false otherwise.
     */
    fun stop(): Boolean {
        return try {
            val stop = plugpag.stopNFCCardDirectly()
            return (stop == PlugPag.NFC_RET_OK)
        } catch (e: PlugPagException) {
            false
        }
    }
}
