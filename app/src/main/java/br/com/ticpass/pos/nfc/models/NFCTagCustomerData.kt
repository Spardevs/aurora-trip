package br.com.ticpass.pos.nfc.models

data class NFCTagCustomerData(
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
    val pin: String = (1000..9999).random().toString(10)
)

data class NFCTagCustomerDataInput(
    val id: String,

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
)