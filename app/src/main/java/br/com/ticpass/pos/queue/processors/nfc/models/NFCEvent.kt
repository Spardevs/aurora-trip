package br.com.ticpass.pos.queue.processors.nfc.models

import br.com.ticpass.pos.queue.core.BaseProcessingEvent

/**
 * NFC-specific events emitted during nfc processing
 */
sealed class NFCEvent : BaseProcessingEvent {

    /**
     * Processor is authenticating sectors of the NFC tag.
     */
    object AUTHENTICATING_SECTORS : NFCEvent()

    /**
     * NFC tag is being formatted.
     */
    object FORMATTING_TAG : NFCEvent()

    /**
     * NFC processing has started.
     */
    object START : NFCEvent()

    /**
     * NFC process was canceled by user or system.
     */
    object CANCELLED : NFCEvent()

    /**
     * NFC is being processed.
     */
    object PROCESSING : NFCEvent()

    /**
     * Validating sector keys for the NFC tag.
     */
    object VALIDATING_SECTOR_KEYS : NFCEvent()

    /**
     * Reading customer data from the NFC tag.
     */
    object READING_TAG_CUSTOMER_DATA : NFCEvent()

    /**
     * Processing tag customer data.
     */
    object PROCESSING_TAG_CUSTOMER_DATA : NFCEvent()

    /**
     * Saving customer data to the NFC tag.
     */
    object SAVING_TAG_CUSTOMER_DATA : NFCEvent()

    /**
     * Reading cart data from the NFC tag.
     */
    object READING_TAG_CART_DATA : NFCEvent()

    /**
     * Processing cart data.
     */
    object PROCESSING_TAG_CART_DATA : NFCEvent()

    /**
     * Writing cart data to the NFC tag.
     */
    object WRITING_TAG_CART_DATA : NFCEvent()

    /**
     * User should place the NFC tag on the reader.
     */
    data class REACH_TAG(
        val timeoutMs: Long = 5000L,
    ) : NFCEvent()
}
