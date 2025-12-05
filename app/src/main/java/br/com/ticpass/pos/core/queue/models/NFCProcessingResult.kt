package br.com.ticpass.pos.core.queue.models

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * Processing Result
 * Represents the possible outcomes when processing an NFC item
 */
sealed class NFCSuccess : ProcessingResult.Success() {
    /**
     * NFC Auth operation success with authentication details
     */
    class CustomerAuthSuccess(
        val id: String,
        val name: String,
        val nationalId: String,
        val phone: String,
        val subjectId: String
    ) : NFCSuccess()
    
    /**
     * NFC Setup operation success with configuration details
     */
    class CustomerSetupSuccess(
        val id: String,
        val name: String,
        val nationalId: String,
        val phone: String,
        val pin: String,
        val subjectId: String
    ) : NFCSuccess()
    
    /**
     * NFC Format operation success with reset details
     */
    class FormatSuccess() : NFCSuccess()
    
    /**
     * NFC Cart Read operation success with cart items
     */
    class CartReadSuccess(
        val items: List<br.com.ticpass.pos.core.nfc.models.NFCCartItem>
    ) : NFCSuccess()
    
    /**
     * NFC Cart Update operation success with updated cart items
     */
    class CartUpdateSuccess(
        val items: List<br.com.ticpass.pos.core.nfc.models.NFCCartItem>
    ) : NFCSuccess()
}

class NFCError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)