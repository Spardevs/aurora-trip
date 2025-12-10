package br.com.ticpass.pos.core.nfc.models

data class NFCTagCustomerData(
    /**
     * Unique identifier derived from the NFC tag's hardware UID.
     * This is a hex string representation of the tag's UID (e.g., "04A1B2C3D4").
     * Not stored in the JSON payload - extracted directly from sector 0, block 0.
     */
    val id: String,

    /**
     * The name of the customer.
     */
    val name: String,

    /**
     * The national ID of the customer, which could be CPF, CNPJ or SSN.
     */
    val nationalId: String,

    /**
     * The phone number of the customer.
     */
    val phone: String,

    /**
     * The pin code for the customer, which should be a 4-digit number.
     */
    val pin: String = (1000..9999).random().toString(10),

    /**
     * The subject ID that this card is associated with (e.g., menu ID, location ID).
     * Used to prevent cross-subject usage (e.g., using a card from menu A on menu B).
     */
    val subjectId: String
)

/**
 * Input data for creating a new NFC customer tag.
 * Note: The tag ID is not included here as it's derived from the hardware UID.
 */
data class NFCTagCustomerDataInput(
    /**
     * The name of the customer.
     */
    val name: String = "",

    /**
     * The national ID of the customer, which could be CPF, CNPJ or SSN.
     * Optional field, there's no need to store dots or dashes.
     */
    val nationalId: String = "",

    /**
     * The phone number of the customer.
     * Optional field, there's no need to store dots or dashes.
     */
    val phone: String = "",

    /**
     * The subject ID that this card is associated with (e.g., menu ID, location ID).
     */
    val subjectId: String
)