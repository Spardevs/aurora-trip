package br.com.ticpass.pos.queue.processors.nfc.models

import br.com.ticpass.pos.R

/**
 * Maps NFCEvent types to string resource keys
 * This centralized mapping makes it easy to maintain error message mappings
 */
object NFCEventResourceMapper {
    
    /**
     * Get the string resource key for a NFCEvent
     * @param event The NFCEvent to map
     * @return The string resource key corresponding to the event
     */
    fun getErrorResourceKey(event: NFCEvent): Int {
        return when (event) {
            is NFCEvent.REACH_TAG -> R.string.event_nfc_reach_tag
            is NFCEvent.START -> R.string.event_start
            is NFCEvent.CANCELLED -> R.string.event_cancelled
            is NFCEvent.PROCESSING -> R.string.event_nfc_processing
            is NFCEvent.VALIDATING_SECTOR_KEYS -> R.string.event_nfc_validating_sector_keys
            is NFCEvent.PROCESSING_TAG_CUSTOMER_DATA -> R.string.event_nfc_processing_tag_customer_data
            is NFCEvent.SAVING_TAG_CUSTOMER_DATA -> R.string.event_nfc_saving_tag_customer_data
            is NFCEvent.READING_TAG_CUSTOMER_DATA -> R.string.event_nfc_reading_tag_customer_data
            NFCEvent.AUTHENTICATING_SECTORS -> R.string.event_nfc_authenticating_sectors
            NFCEvent.FORMATTING_TAG -> R.string.event_nfc_formatting_tag
        }
    }
}
