package br.com.ticpass.pos.queue.processors.nfc.processors.models

/**
 * Enum defining the types of nfc processors available in the system
 */
enum class NFCProcessorType {
    CUSTOMER_AUTH,
    TAG_FORMAT,
    CUSTOMER_SETUP,
    CART_READ,
    CART_UPDATE,
}
