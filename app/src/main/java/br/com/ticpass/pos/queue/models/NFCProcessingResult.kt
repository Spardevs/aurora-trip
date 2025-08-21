package br.com.ticpass.pos.queue.models

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

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
    ) : NFCSuccess()
    
    /**
     * NFC Format operation success with reset details
     */
    class FormatSuccess() : NFCSuccess()
}

class NFCError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)