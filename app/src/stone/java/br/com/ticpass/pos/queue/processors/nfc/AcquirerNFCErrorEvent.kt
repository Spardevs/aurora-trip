package br.com.ticpass.pos.queue.processors.nfc

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import stone.application.enums.ErrorsEnum
import stone.application.enums.TransactionStatusEnum

/**
 * Stone-specific error events emitted during payment processing
 */
enum class AcquirerNFCErrorEvent(val code: ErrorsEnum, val event: ProcessingErrorEvent) {
    // Card-related errors
    CARD_BLOCKED(
        ErrorsEnum.CARD_BLOCKED,
        ProcessingErrorEvent.BLOCKED_CARD
    ),

    CARD_READ_TIMEOUT(
        ErrorsEnum.CARD_READ_TIMEOUT_ERROR,
        ProcessingErrorEvent.OPERATION_TIMEOUT
    ),

    CARD_UNSUPPORTED(
        ErrorsEnum.CARD_UNSUPPORTED_ERROR,
        ProcessingErrorEvent.INVALID_CARD_DATA
    ),

    // Connection and device errors
    CONNECTION_NOT_FOUND(
        ErrorsEnum.CONNECTION_NOT_FOUND,
        ProcessingErrorEvent.CONNECTION_ERROR
    ),

    PINPAD_WITHOUT_KEY(
        ErrorsEnum.PINPAD_WITHOUT_KEY,
        ProcessingErrorEvent.READER_WITHOUT_KEY
    ),

    PINPAD_WITHOUT_STONE_KEY(
        ErrorsEnum.PINPAD_WITHOUT_STONE_KEY,
        ProcessingErrorEvent.READER_WITHOUT_KEY
    ),

    CONNECTIVITY_ERROR(
        ErrorsEnum.CONNECTIVITY_ERROR,
        ProcessingErrorEvent.CONNECTION_ERROR
    ),

    // Transaction errors
    INVALID_TRANSACTION(
        ErrorsEnum.INVALID_TRANSACTION,
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    INVALID_AMOUNT(
        ErrorsEnum.INVALID_AMOUNT,
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    INVALID_TRANSACTION_STATUS(
        ErrorsEnum.INVALID_TRANSACTION_STATUS,
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    TRANSACTION_NOT_FOUND(
        ErrorsEnum.TRANSACTION_NOT_FOUND,
        ProcessingErrorEvent.TRANSACTION_NOT_FOUND
    ),

    TRANS_GENERIC_ERROR(
        ErrorsEnum.TRANS_GENERIC_ERROR,
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    TRANS_INVALID_AMOUNT(
        ErrorsEnum.TRANS_INVALID_AMOUNT_ERROR,
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    TRANS_SELECT_TYPE_USER_CANCELED(
        ErrorsEnum.TRANS_SELECT_TYPE_USER_CANCELED_ERROR,
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),

    TRANS_NO_TRANS_TYPE(
        ErrorsEnum.TRANS_NO_TRANS_TYPE_ERROR,
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    TRANS_WRONG_TRANS_TYPE(
        ErrorsEnum.TRANS_WRONG_TRANS_TYPE_ERROR,
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    TRANS_APP_INVALID(
        ErrorsEnum.TRANS_APP_INVALID_ERROR,
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    TRANS_APP_INVALID_INDEX(
        ErrorsEnum.TRANS_APP_INVALID_INDEX_ERROR,
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    TRANS_ONLINE_PROCESS_ERROR(
        ErrorsEnum.TRANS_ONLINE_PROCESS_ERROR_ERROR,
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    // Timeout and user interaction errors
    TIME_OUT(
        ErrorsEnum.TIME_OUT,
        ProcessingErrorEvent.OPERATION_TIMEOUT
    ),

    OPERATION_CANCELLED_BY_USER(
        ErrorsEnum.OPERATION_CANCELLED_BY_USER,
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),

    // Printer errors
    PRINTER_GENERIC_ERROR(
        ErrorsEnum.PRINTER_GENERIC_ERROR,
        ProcessingErrorEvent.PRINTER_ERROR
    ),

    PRINTER_BUSY_ERROR(
        ErrorsEnum.PRINTER_BUSY_ERROR,
        ProcessingErrorEvent.PRINTER_BUSY
    ),

    PRINTER_OUT_OF_PAPER(
        ErrorsEnum.PRINTER_OUT_OF_PAPER_ERROR,
        ProcessingErrorEvent.PRINTER_OUT_OF_PAPER
    ),

    PRINTER_LOW_ENERGY(
        ErrorsEnum.PRINTER_LOW_ENERGY_ERROR,
        ProcessingErrorEvent.LOW_BATTERY
    ),

    PRINTER_OVERHEATING(
        ErrorsEnum.PRINTER_OVERHEATING_ERROR,
        ProcessingErrorEvent.PRINTER_OVERHEATING
    ),

    // Generic and system errors
    GENERIC_ERROR(
        ErrorsEnum.GENERIC_ERROR,
        ProcessingErrorEvent.GENERIC
    ),

    UNKNOWN_ERROR(
        ErrorsEnum.UNKNOWN_ERROR,
        ProcessingErrorEvent.GENERIC
    ),

    UNEXPECTED_STATUS_COMMAND(
        ErrorsEnum.UNEXPECTED_STATUS_COMMAND,
        ProcessingErrorEvent.UNEXPECTED_ERROR
    ),

    ERROR_RESPONSE_COMMAND(
        ErrorsEnum.ERROR_RESPONSE_COMMAND,
        ProcessingErrorEvent.ERROR_RESPONSE
    ),

    ACCEPTOR_REJECTION(
        ErrorsEnum.ACCEPTOR_REJECTION,
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_ACQUIRER
    ),

    // Fallback and transaction flow errors
    SWIPE_INCORRECT(
        ErrorsEnum.SWIPE_INCORRECT,
        ProcessingErrorEvent.SWIPE_INCORRECT
    ),

    // Configuration and setup errors
    COULD_NOT_ACTIVATE_WITH_ACCEPTOR_CONFIGURATION_UPDATE_DATA_NULL(
        ErrorsEnum.COULD_NOT_ACTIVATE_WITH_ACCEPTOR_CONFIGURATION_UPDATE_DATA_NULL,
        ProcessingErrorEvent.DEVICE_NOT_ACTIVATED
    ),

    APPNAME_NOT_SET(
        ErrorsEnum.APPNAME_NOT_SET,
        ProcessingErrorEvent.APP_NAME_NOT_SET
    ),

    // Additional card errors
    CARD_READ_ERROR(
        ErrorsEnum.CARD_READ_ERROR,
        ProcessingErrorEvent.CARD_READ_ERROR
    ),

    CARD_READ_CANCELED(
        ErrorsEnum.CARD_READ_CANCELED_ERROR,
        ProcessingErrorEvent.CARD_READ_CANCELED
    ),

    CARD_READ_MULTI(
        ErrorsEnum.CARD_READ_MULTI_ERROR,
        ProcessingErrorEvent.CARD_READ_MULTI_ERROR
    ),

    CANT_READ_CARD_HOLDER_INFORMATION(
        ErrorsEnum.CANT_READ_CARD_HOLDER_INFORMATION,
        ProcessingErrorEvent.CARD_HOLDER_READ_ERROR
    ),

    // Additional connection and device errors

    PINPAD_CONNECTION_NOT_FOUND(
        ErrorsEnum.PINPAD_CONNECTION_NOT_FOUND,
        ProcessingErrorEvent.PINPAD_CONNECTION_NOT_FOUND
    ),

    PINPAD_ALREADY_CONNECTED(
        ErrorsEnum.PINPAD_ALREADY_CONNECTED,
        ProcessingErrorEvent.PINPAD_ALREADY_CONNECTED
    ),

    PINPAD_CLOSED_CONNECTION(
        ErrorsEnum.PINPAD_CLOSED_CONNECTION,
        ProcessingErrorEvent.PINPAD_CLOSED_CONNECTION
    ),

    IO_ERROR_WITH_PINPAD(
        ErrorsEnum.IO_ERROR_WITH_PINPAD,
        ProcessingErrorEvent.IO_ERROR_WITH_PINPAD
    ),

    // Additional transaction errors
    TRANS_APP_BLOCKED(
        ErrorsEnum.TRANS_APP_BLOCKED_ERROR,
        ProcessingErrorEvent.TRANSACTION_APP_BLOCKED
    ),

    TRANS_CVV_NOT_PROVIDED(
        ErrorsEnum.TRANS_CVV_NOT_PROVIDED_ERROR,
        ProcessingErrorEvent.CVV_NOT_PROVIDED
    ),

    TRANS_CVV_INVALID(
        ErrorsEnum.TRANS_CVV_INVALID_ERROR,
        ProcessingErrorEvent.CVV_INVALID
    ),

    // EMV errors
    EMV_GENERIC_ERROR(
        ErrorsEnum.EMV_GENERIC_ERROR,
        ProcessingErrorEvent.EMV_PROCESSING_ERROR
    ),

    EMV_FAILED_CARD_CONN_ERROR(
        ErrorsEnum.EMV_FAILED_CARD_CONN_ERROR,
        ProcessingErrorEvent.EMV_CARD_CONNECTION_ERROR
    ),

    EMV_NO_APP_ERROR(
        ErrorsEnum.EMV_NO_APP_ERROR,
        ProcessingErrorEvent.EMV_NO_APPLICATION
    ),

    EMV_INITIALIZATION_ERROR(
        ErrorsEnum.EMV_INITIALIZATION_ERROR,
        ProcessingErrorEvent.INITIALIZATION_ERROR
    ),

    EMV_CAPK_ERROR(
        ErrorsEnum.EMV_CAPK_ERROR,
        ProcessingErrorEvent.EMV_CAPK_ERROR
    ),

    EMV_TLV_ERROR(
        ErrorsEnum.EMV_TLV_ERROR,
        ProcessingErrorEvent.EMV_TLV_ERROR
    ),

    EMV_NO_CAPK_ERROR(
        ErrorsEnum.EMV_NO_CAPK_ERROR,
        ProcessingErrorEvent.EMV_CAPK_ERROR
    ),

    EMV_AID_ERROR(
        ErrorsEnum.EMV_AID_ERROR,
        ProcessingErrorEvent.EMV_AID_ERROR
    ),

    // PED/PIN errors
    PED_PASS_GENERIC_ERROR(
        ErrorsEnum.PED_PASS_GENERIC_ERROR,
        ProcessingErrorEvent.PIN_ENTRY_ERROR
    ),

    PED_PASS_KEY_ERROR(
        ErrorsEnum.PED_PASS_KEY_ERROR,
        ProcessingErrorEvent.PIN_KEY_ERROR
    ),

    PED_PASS_USER_CANCELED(
        ErrorsEnum.PED_PASS_USER_CANCELED_ERROR,
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),

    PED_PASS_NO_PIN_INPUT(
        ErrorsEnum.PED_PASS_NO_PIN_INPUT_ERROR,
        ProcessingErrorEvent.PIN_NO_INPUT
    ),

    PED_PASS_TIMEOUT(
        ErrorsEnum.PED_PASS_TIMEOUT_ERROR,
        ProcessingErrorEvent.OPERATION_TIMEOUT
    ),

    PED_PASS_INIT_ERROR(
        ErrorsEnum.PED_PASS_INIT_ERROR,
        ProcessingErrorEvent.PIN_INITIALIZATION_ERROR
    ),

    PED_PASS_CRYPT_ERROR(
        ErrorsEnum.PED_PASS_CRYPT_ERROR,
        ProcessingErrorEvent.PIN_ENCRYPTION_ERROR
    ),

    PED_PASS_NO_KEY_FOUND(
        ErrorsEnum.PED_PASS_NO_KEY_FOUND_ERROR,
        ProcessingErrorEvent.PIN_KEY_NOT_FOUND
    ),

    // Additional system errors
    NULL_RESPONSE(
        ErrorsEnum.NULL_RESPONSE,
        ProcessingErrorEvent.NULL_RESPONSE
    ),

    // Mifare/NFC errors
    NO_MIFARE_SUPPORT(
        ErrorsEnum.NO_MIFARE_SUPPORT,
        ProcessingErrorEvent.NFC_NOT_SUPPORTED
    ),

    MIFARE_ABORTED(
        ErrorsEnum.MIFARE_ABORTED,
        ProcessingErrorEvent.NFC_OPERATION_ABORTED
    ),

    MIFARE_DETECT_TIMEOUT(
        ErrorsEnum.MIFARE_DETECT_TIMEOUT,
        ProcessingErrorEvent.OPERATION_TIMEOUT
    ),

    MIFARE_WRONG_CARD_TYPE(
        ErrorsEnum.MIFARE_WRONG_CARD_TYPE,
        ProcessingErrorEvent.NFC_WRONG_CARD_TYPE
    ),

    MIFARE_INVALID_KEY(
        ErrorsEnum.MIFARE_INVALID_KEY,
        ProcessingErrorEvent.NFC_INVALID_KEY
    ),

    MIFARE_NOT_AUTHENTICATED(
        ErrorsEnum.MIFARE_NOT_AUTHENTICATED,
        ProcessingErrorEvent.NFC_NOT_AUTHENTICATED
    ),

    MIFARE_INVALID_SECTOR_NUMBER(
        ErrorsEnum.MIFARE_INVALID_SECTOR_NUMBER,
        ProcessingErrorEvent.INVALID_SECTOR_NUMBER
    ),

    MIFARE_INVALID_BLOCK_NUMBER(
        ErrorsEnum.MIFARE_INVALID_BLOCK_NUMBER,
        ProcessingErrorEvent.INVALID_BLOCK_NUMBER
    ),

    MIFARE_INVALID_BLOCK_FORMAT(
        ErrorsEnum.MIFARE_INVALID_BLOCK_FORMAT,
        ProcessingErrorEvent.INVALID_BLOCK_FORMAT
    ),

    MIFARE_MULTI_CARD_DETECTED(
        ErrorsEnum.MIFARE_MULTI_CARD_DETECTED,
        ProcessingErrorEvent.TOO_MANY_CARDS
    ),

    // QR Code errors
    QRCODE_NOT_GENERATED(
        ErrorsEnum.QRCODE_NOT_GENERATED,
        ProcessingErrorEvent.QRCODE_GENERATION_ERROR
    ),

    QRCODE_EXPIRED(
        ErrorsEnum.QRCODE_EXPIRED,
        ProcessingErrorEvent.QRCODE_EXPIRED
    ),

    // Fallback and transaction flow errors
    TRANSACTION_FALLBACK_STARTED(
        ErrorsEnum.TRANSACTION_FALLBACK_STARTED,
        ProcessingErrorEvent.TRANSACTION_FALLBACK
    ),

    TRANSACTION_FALLBACK_TIMEOUT(
        ErrorsEnum.TRANSACTION_FALLBACK_TIMEOUT,
        ProcessingErrorEvent.OPERATION_TIMEOUT
    ),

    TRANSACTION_FALLBACK_INVALID_CARD_MODE(
        ErrorsEnum.TRANSACTION_FALLBACK_INVALID_CARD_MODE,
        ProcessingErrorEvent.INVALID_CARD_MODE
    ),

    PASS_TARGE_WITH_CHIP(
        ErrorsEnum.PASS_TARGE_WITH_CHIP,
        ProcessingErrorEvent.USE_CHIP_FOR_TRANSACTION
    ),

    TOO_MANY_CARDS(
        ErrorsEnum.TOO_MANY_CARDS,
        ProcessingErrorEvent.TOO_MANY_CARDS
    ),

    // Additional configuration and setup errors
    NOT_STONE_POS_OR_POS_MISCONFIGURED(
        ErrorsEnum.NOT_STONE_POS_OR_POS_MISCONFIGURED,
        ProcessingErrorEvent.DEVICE_MISCONFIGURED
    ),

    COULD_NOT_ACTIVATE_ALL_STONE_CODES(
        ErrorsEnum.COULD_NOT_ACTIVATE_ALL_STONE_CODES,
        ProcessingErrorEvent.ACTIVATION_ERROR
    ),

    SDK_VERSION_OUTDATED(
        ErrorsEnum.SDK_VERSION_OUTDATED,
        ProcessingErrorEvent.SDK_VERSION_OUTDATED
    ),

    NO_ACTIVE_APPLICATION(
        ErrorsEnum.NO_ACTIVE_APPLICATION,
        ProcessingErrorEvent.NO_ACTIVE_APPLICATION
    ),

    MULTI_INSTANCES_OF_PROVIDER_RUNNING(
        ErrorsEnum.MULTI_INSTANCES_OF_PROVIDER_RUNNING,
        ProcessingErrorEvent.MULTIPLE_PROVIDER_INSTANCES
    ),

    UNKNOWN_TYPE_OF_USER(
        ErrorsEnum.UNKNOWN_TYPE_OF_USER,
        ProcessingErrorEvent.UNKNOWN_USER_TYPE
    ),

    TRANSACTION_OBJECT_NULL_ERROR(
        ErrorsEnum.TRANSACTION_OBJECT_NULL_ERROR,
        ProcessingErrorEvent.TRANSACTION_OBJECT_NULL
    ),

    // Email errors
    EMAIL_MESSAGE_ERROR(
        ErrorsEnum.EMAIL_MESSAGE_ERROR,
        ProcessingErrorEvent.EMAIL_ERROR
    ),

    INVALID_EMAIL_CLIENT(
        ErrorsEnum.INVALID_EMAIL_CLIENT,
        ProcessingErrorEvent.EMAIL_CLIENT_ERROR
    ),

    EMAIL_EMPTY(
        ErrorsEnum.EMAIL_EMPTY,
        ProcessingErrorEvent.EMAIL_EMPTY
    ),

    EMAIL_RECIPIENT_EMPTY(
        ErrorsEnum.EMAIL_RECIPIENT_EMPTY,
        ProcessingErrorEvent.EMAIL_RECIPIENT_EMPTY
    ),

    // Data container errors
    DATA_CONTAINER_CONSTRAINT_ERROR(
        ErrorsEnum.DATA_CONTAINER_CONSTRAINT_ERROR,
        ProcessingErrorEvent.DATA_CONSTRAINT_ERROR
    ),

    DATA_CONTAINER_INTEGRATION_ERROR(
        ErrorsEnum.DATA_CONTAINER_INTEGRATION_ERROR,
        ProcessingErrorEvent.DATA_INTEGRATION_ERROR
    ),

    // Switch interface
    SWITCH_INTERFACE(
        ErrorsEnum.SWITCH_INTERFACE,
        ProcessingErrorEvent.USE_CHIP_FOR_TRANSACTION
    ),

    // Additional card errors
    CARD_REMOVED_BY_USER(
        ErrorsEnum.CARD_REMOVED_BY_USER,
        ProcessingErrorEvent.CARD_REMOVED_BY_USER
    ),

    CANT_READ_CHIP_CARD(
        ErrorsEnum.CANT_READ_CHIP_CARD,
        ProcessingErrorEvent.CHIP_CARD_READ_ERROR
    ),

    CARD_GENERIC_ERROR(
        ErrorsEnum.CARD_GENERIC_ERROR,
        ProcessingErrorEvent.CARD_GENERIC_ERROR
    ),

    // Additional connection and device errors
    DEVICE_NOT_COMPATIBLE(
        ErrorsEnum.DEVICE_NOT_COMPATIBLE,
        ProcessingErrorEvent.DEVICE_NOT_COMPATIBLE
    ),

    // Additional transaction errors
    TRANS_PASS_MAG_BUT_IS_ICC(
        ErrorsEnum.TRANS_PASS_MAG_BUT_IS_ICC_ERROR,
        ProcessingErrorEvent.MAG_STRIPE_CHIP_DETECTED
    ),

    // Authentication and security errors
    INVALID_STONECODE(
        ErrorsEnum.INVALID_STONECODE,
        ProcessingErrorEvent.INVALID_ACQUIRER_ACTIVATION_CODE
    ),

    USERMODEL_NOT_FOUND(
        ErrorsEnum.USERMODEL_NOT_FOUND,
        ProcessingErrorEvent.USER_MODEL_NOT_FOUND
    ),

    INVALID_STONE_CODE_OR_UNKNOWN(
        ErrorsEnum.INVALID_STONE_CODE_OR_UNKNOWN,
        ProcessingErrorEvent.INVALID_OR_UNKNOWN_STONE_CODE
    ),

    // Additional printer errors
    PRINTER_INIT_ERROR(
        ErrorsEnum.PRINTER_INIT_ERROR,
        ProcessingErrorEvent.PRINTER_INITIALIZATION_ERROR
    ),

    PRINTER_UNSUPPORTED_FORMAT(
        ErrorsEnum.PRINTER_UNSUPPORTED_FORMAT_ERROR,
        ProcessingErrorEvent.PRINTER_UNSUPPORTED_FORMAT
    ),

    PRINTER_INVALID_DATA(
        ErrorsEnum.PRINTER_INVALID_DATA_ERROR,
        ProcessingErrorEvent.PRINTER_INVALID_DATA
    ),

    NO_PRINT_SUPPORT(
        ErrorsEnum.NO_PRINT_SUPPORT,
        ProcessingErrorEvent.NO_PRINT_SUPPORT
    ),

    // Additional system errors
    INTERNAL_ERROR(
        ErrorsEnum.INTERNAL_ERROR,
        ProcessingErrorEvent.INTERNAL_SYSTEM_ERROR
    ),

    TABLES_NOT_FOUND(
        ErrorsEnum.TABLES_NOT_FOUND,
        ProcessingErrorEvent.TABLES_NOT_FOUND
    ),

    NEED_LOAD_TABLES(
        ErrorsEnum.NEED_LOAD_TABLES,
        ProcessingErrorEvent.NEED_LOAD_TABLES
    );

    companion object {
        /**
         * Translates a Stone ErrorsEnum to a ProcessingErrorEvent.
         * @param code The error code from Stone SDK.
         * @return The corresponding ProcessingErrorEvent.
         */
        fun translate(code: ErrorsEnum): ProcessingErrorEvent {
            return entries.find { it.code == code }?.event ?: ProcessingErrorEvent.GENERIC
        }

        /**
         * Translates a ProcessingErrorEvent to its Stone corresponding error code.
         * @param event The ProcessingErrorEvent to translate.
         * @return The corresponding error code.
         */
        fun translate(event: ProcessingErrorEvent): ErrorsEnum? {
            return entries.find { it.event == event }?.code
        }
    }
}
