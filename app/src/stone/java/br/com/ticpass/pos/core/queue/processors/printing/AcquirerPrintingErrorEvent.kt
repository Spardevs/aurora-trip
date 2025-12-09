package br.com.ticpass.pos.core.queue.processors.printing

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import stone.application.enums.ErrorsEnum
import stone.application.enums.TransactionStatusEnum

/**
 * Stone-specific error events emitted during payment processing
 */
enum class AcquirerPrintingErrorEvent(val code: ErrorsEnum, val event: ProcessingErrorEvent) {

    CONNECTIVITY_ERROR(
        ErrorsEnum.CONNECTIVITY_ERROR,
        ProcessingErrorEvent.CONNECTION_ERROR
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

    APPNAME_NOT_SET(
        ErrorsEnum.APPNAME_NOT_SET,
        ProcessingErrorEvent.APP_NAME_NOT_SET
    ),

    // Additional card errors
    CARD_READ_ERROR(
        ErrorsEnum.CARD_READ_ERROR,
        ProcessingErrorEvent.CARD_READ_ERROR
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

    // Additional connection and device errors
    DEVICE_NOT_COMPATIBLE(
        ErrorsEnum.DEVICE_NOT_COMPATIBLE,
        ProcessingErrorEvent.DEVICE_NOT_COMPATIBLE
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
