package br.com.ticpass.pos.core.queue.processors.printing

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

/**
 * PagSeguro-specific error events emitted during printing processing
 */
enum class AcquirerPrintingErrorEvent(val code: String, val event: ProcessingErrorEvent) {
    /**
     * Operation time exceeded.
     * Seller should process the sale again.
     */
    OPERATION_TIME_EXCEEDED_S09("S09", ProcessingErrorEvent.OPERATION_TIME_EXCEEDED),
    
    /**
     * Unexpected error.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    UNEXPECTED_ERROR_S12("S12", ProcessingErrorEvent.UNEXPECTED_ERROR),

    /**
     * Unexpected error.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    UNEXPECTED_ERROR_S17("S17", ProcessingErrorEvent.UNEXPECTED_ERROR),

    /**
     * Terminal not found.
     * Check terminal registration in the system.
     */
    TERMINAL_NOT_FOUND_S26("S26", ProcessingErrorEvent.TERMINAL_NOT_FOUND),

    /**
     * Invalid POS key.
     * Contact support.
     */
    INVALID_POS_KEY_S33("S33", ProcessingErrorEvent.INVALID_POS_KEY),

    /**
     * Account has been closed.
     * Contact support.
     */
    ACCOUNT_CLOSED_M2042("M2042", ProcessingErrorEvent.ACCOUNT_CLOSED),

    /**
     * Update the app version to proceed with the operation.
     */
    UPDATE_APP_VERSION_M2054("M2054", ProcessingErrorEvent.UPDATE_APP),
    
    /**
     * Operation not performed.
     */
    OPERATION_NOT_PERFORMED_M2060("M2060", ProcessingErrorEvent.OPERATION_NOT_PERFORMED),
    
    /**
     * To continue selling, update your device.
     */
    UPDATE_DEVICE_M2069("M2069", ProcessingErrorEvent.UPDATE_DEVICE),    SELLER_BLOCKED_M3013("M3013", ProcessingErrorEvent.SELLER_BLOCKED),
    
    /**
     * Try again.
     */
    TRY_AGAIN_M3017("M3017", ProcessingErrorEvent.TRY_AGAIN),

    /**
     * Please log in to the app again.
     * Redo the terminal activation.
     */
    PLEASE_LOGIN_AGAIN_M3021("M3021", ProcessingErrorEvent.PLEASE_LOGIN_AGAIN),

    /**
     * Table loading error.
     * Try making a sale to force the loading.
     */
    TABLE_LOADING_ERROR_B16("B16", ProcessingErrorEvent.TABLE_LOADING_ERROR),
    
    /**
     * Operation cancelled.
     * Try again.
     */
    OPERATION_CANCELLED_B18("B18", ProcessingErrorEvent.OPERATION_CANCELLED),

    /**
     * Response time exceeded.
     * Try again.
     */
    RESPONSE_TIME_EXCEEDED_B25("B025", ProcessingErrorEvent.RESPONSE_TIME_EXCEEDED),

    /**
     * Operation not performed.
     * Check if the card has the chosen payment method enabled.
     */
    OPERATION_NOT_PERFORMED_B41("B41", ProcessingErrorEvent.OPERATION_NOT_PERFORMED),


    /**
     * Maximum time limit for operation exceeded.
     * Card was not inserted.
     */
    MAX_TIME_EXCEEDED_C12("C12", ProcessingErrorEvent.MAX_TIME_EXCEEDED),
    
    /**
     * Operation cancelled.
     * Try again.
     */
    OPERATION_CANCELLED_C13("C13", ProcessingErrorEvent.OPERATION_CANCELLED),
    
    /**
     * Feature not available in pinpad library.
     * Data processing failure, ask the seller to try again.
     */
    FUNCTION_NOT_AVAILABLE_C18("C18", ProcessingErrorEvent.FEATURE_UNAVAILABLE),
    
    /**
     * Invalid POS key.
     * Equipment should be replaced.
     */
    INVALID_POS_KEY_C33("C33", ProcessingErrorEvent.INVALID_POS_KEY),


    /**
     * Operation timeout.
     * Operation took too long to process.
     */
    OPERATION_TIMEOUT_A240("A240", ProcessingErrorEvent.OPERATION_TIMEOUT),
    
    /**
     * Operation canceled by user.
     * User canceled the operation.
     */
    OPERATION_CANCELED_BY_USER_A242("A242", ProcessingErrorEvent.CANCELLED_BY_USER),
    
    /**
     * File operation failure.
     * Try again or restart terminal.
     */
    FILE_OPERATION_FAILURE_A243("A243", ProcessingErrorEvent.FILE_OPERATION_FAILURE),
    
    /**
     * File operation failure.
     * Try again or restart terminal.
     */
    FILE_OPERATION_FAILURE_A244("A244", ProcessingErrorEvent.FILE_OPERATION_FAILURE),
    
    /**
     * File operation failure.
     * Try again or restart terminal.
     */
    FILE_OPERATION_FAILURE_A245("A245", ProcessingErrorEvent.FILE_OPERATION_FAILURE),

    /**
     * Printer busy.
     * Wait for printer to finish and try again.
     */
    PRINTER_BUSY_A249("A249", ProcessingErrorEvent.PRINTER_BUSY),
    
    /**
     * Print data format error.
     * Try another file format.
     */
    PRINT_DATA_FORMAT_ERROR_A251("A251", ProcessingErrorEvent.PRINT_DATA_FORMAT_ERROR),
    
    /**
     * Printer malfunction.
     * Try again or check printer hardware.
     */
    PRINTER_DEFECTIVE_A252("A252", ProcessingErrorEvent.PRINTER_MALFUNCTION),
    
    /**
     * Printer overheating.
     * Try again later.
     */
    PRINTER_OVERHEATING_A253("A253", ProcessingErrorEvent.PRINTER_OVERHEATING),
    
    /**
     * Print error due to low battery.
     * Connect charger and try again.
     */
    PRINT_ERROR_LOW_BATTERY_A254("A254", ProcessingErrorEvent.PRINT_ERROR_LOW_BATTERY),

    /**
     * Print error file not found.
     * File for printing not found.
     */
    PRINT_ERROR_FILE_NOT_FOUND_A256("A256", ProcessingErrorEvent.PRINT_FILE_NOT_FOUND),


    /**
     * Print error due to low battery.
     * Connect charger and try again.
     */
    PRINT_ERROR_LOW_BATTERY_A52("A52", ProcessingErrorEvent.PRINT_ERROR_LOW_BATTERY),
    
    /**
     * Operation canceled by user.
     * User pressed cancel button.
     */
    OPERATION_CANCELED_BY_USER_A202("A202", ProcessingErrorEvent.CANCELLED_BY_USER),
    
    /**
     * Initialization error.
     * Restart terminal.
     */
    INITIALIZATION_ERROR_A205("A205", ProcessingErrorEvent.INITIALIZATION_ERROR),
    
    /**
     * Operation not performed.
     * Try again.
     */
    OPERATION_NOT_PERFORMED_A206("A206", ProcessingErrorEvent.OPERATION_NOT_PERFORMED),
    
    /**
     * Operation not performed.
     * Try again.
     */
    OPERATION_NOT_PERFORMED_A207("A207", ProcessingErrorEvent.OPERATION_NOT_PERFORMED),
    
    /**
     * Incorrect password.
     * Try again.
     */
    INCORRECT_PASSWORD_A208("A208", ProcessingErrorEvent.CHECK_PASSWORD),


    /**
     * Operation canceled.
     * User canceled the operation in progress.
     */
    OPERATION_CANCELED_A01("A01", ProcessingErrorEvent.OPERATION_CANCELLED),
    
    /**
     * Device deactivated.
     * Device is not activated.
     */
    DEVICE_DEACTIVATED_A02("A02", ProcessingErrorEvent.DEVICE_DEACTIVATED),
    
    /**
     * No message to display.
     * No last error message to display.
     */
    NO_MESSAGE_A03("A03", ProcessingErrorEvent.NO_MESSAGE),
    
    /**
     * Operation not performed.
     * Transaction denied by host.
     */
    OPERATION_NOT_PERFORMED_A04("A04", ProcessingErrorEvent.OPERATION_NOT_PERFORMED),


    /**
     * Printer out of paper.
     * Replace the paper roll.
     */
    PRINTER_OUT_OF_PAPER_A21("A21", ProcessingErrorEvent.PRINTER_OUT_OF_PAPER),
    
    /**
     * Operation not authorized.
     * Operation not authorized by host.
     */
    OPERATION_NOT_AUTHORIZED_A22("A22", ProcessingErrorEvent.OPERATION_NOT_AUTHORIZED),


    /**
     * Overvoltage problem.
     * Printer reporting overvoltage.
     */
    OVERVOLTAGE_PROBLEM_A26("A26", ProcessingErrorEvent.PRINTER_OVERVOLTAGE),
    
    /**
     * Overheating problem.
     * Printer reporting overheating.
     */
    OVERHEATING_PROBLEM_A27("A27", ProcessingErrorEvent.PRINTER_OVERHEATING),


    /**
     * Unexpected error.
     * Try again or contact support with transaction details.
     */
    UNEXPECTED_ERROR(
        "S01",
        ProcessingErrorEvent.UNEXPECTED_ERROR
    ),

    /**
     * Operation time exceeded.
     * Try processing the transaction again.
     */
    OPERATION_TIME_EXCEEDED(
        "S02",
        ProcessingErrorEvent.OPERATION_TIME_EXCEEDED
    ),

    /**
     * Unexpected error.
     * Try again or contact support with transaction details.
     */
    UNEXPECTED_ERROR_S04(
        "S04",
        ProcessingErrorEvent.UNEXPECTED_ERROR
    ),

    /**
     * Unexpected error.
     * Try again or contact support with transaction details.
     */
    UNEXPECTED_ERROR_S05(
        "S05",
        ProcessingErrorEvent.UNEXPECTED_ERROR
    ),

    /**
     * Unexpected error.
     * Try again or contact support with transaction details.
     */
    UNEXPECTED_ERROR_S06(
        "S06",
        ProcessingErrorEvent.UNEXPECTED_ERROR
    ),

    /**
     * Unexpected error.
     * Try again or contact support with transaction details.
     */
    UNEXPECTED_ERROR_S07(
        "S07",
        ProcessingErrorEvent.UNEXPECTED_ERROR
    ),

    /**
     * Terminal not found.
     * Verify terminal registration in system.
     */
    TERMINAL_NOT_FOUND(
        "S26",
        ProcessingErrorEvent.TERMINAL_NOT_FOUND
    ),

    /**
     * Invalid POS Key.
     * Contact technical support.
     */
    INVALID_POS_KEY_S32(
        "S32",
        ProcessingErrorEvent.INVALID_POS_KEY
    ),

    /**
     * Invalid POS Key.
     * Contact technical support.
     */
    INVALID_POS_KEY_S96(
        "S96",
        ProcessingErrorEvent.INVALID_POS_KEY
    ),

    /**
     * Function unavailable at the moment.
     * Process through online banking service.
     */
    FUNCTION_UNAVAILABLE(
        "S510",
        ProcessingErrorEvent.FEATURE_UNAVAILABLE
    ),

    /**
     * Operation canceled by user.
     * User initiated cancellation of the operation.
     */
    OPERATION_CANCELED(
        "A01",
        ProcessingErrorEvent.OPERATION_CANCELLED
    ),

    /**
     * Device not activated.
     * Activate the payment terminal.
     */
    DEVICE_NOT_ACTIVATED(
        "A02",
        ProcessingErrorEvent.DEVICE_NOT_ACTIVATED
    ),

    /**
     * No error message available.
     * No previous error to display.
     */
    NO_ERROR_MESSAGE(
        "A03",
        ProcessingErrorEvent.NO_MESSAGE
    ),

    /**
     * Printer out of paper.
     * Replace paper roll and try again.
     */
    PRINTER_OUT_OF_PAPER(
        "A21",
        ProcessingErrorEvent.PRINTER_OUT_OF_PAPER
    ),

    /**
     * Printer overvoltage error.
     * Contact support for assistance.
     */
    PRINTER_OVERVOLTAGE(
        "A26",
        ProcessingErrorEvent.PRINTER_OVERVOLTAGE
    ),

    /**
     * Printer overheating.
     * Allow printer to cool down and try again.
     */
    PRINTER_OVERHEATING(
        "A27",
        ProcessingErrorEvent.PRINTER_OVERHEATING
    ),

    /**
     * Low battery.
     * Connect charger and try again.
     */
    LOW_BATTERY(
        "A52",
        ProcessingErrorEvent.LOW_BATTERY
    ),

    /**
     * Terminal not configured.
     * Configure terminal parameters.
     */
    TERMINAL_NOT_CONFIGURED(
        "A99",
        ProcessingErrorEvent.TERMINAL_NOT_CONFIGURED
    ),

    /**
     * Invalid parameter.
     * Contact support with transaction details.
     */
    INVALID_PARAMETER(
        "A201",
        ProcessingErrorEvent.INVALID_PARAMETER
    ),

    /**
     * Operation canceled by user.
     * User pressed cancel button.
     */
    USER_CANCELED_OPERATION(
        "A202",
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),

    /**
     * Initialization error.
     * Restart terminal and try again.
     */
    INITIALIZATION_ERROR(
        "A205",
        ProcessingErrorEvent.INITIALIZATION_ERROR
    ),

    /**
     * Operation time exceeded.
     * Try processing the transaction again.
     */
    OPERATION_TIME_EXCEEDED_A240(
        "A240",
        ProcessingErrorEvent.OPERATION_TIME_EXCEEDED
    ),

    /**
     * Low battery.
     * Connect charger and try again.
     */
    LOW_BATTERY_A248(
        "A248",
        ProcessingErrorEvent.LOW_BATTERY
    ),

    /**
     * Printer busy.
     * Wait for current print job to complete.
     */
    PRINTER_BUSY(
        "A249",
        ProcessingErrorEvent.PRINTER_BUSY
    ),

    /**
     * Printer out of paper.
     * Replace paper roll and try again.
     */
    PRINTER_OUT_OF_PAPER_A250(
        "A250",
        ProcessingErrorEvent.PRINTER_OUT_OF_PAPER
    ),

    /**
     * Invalid print data format.
     * Try a different file format.
     */
    INVALID_PRINT_FORMAT(
        "A251",
        ProcessingErrorEvent.PRINT_DATA_FORMAT_ERROR
    ),

    /**
     * Printer error.
     * Contact support for assistance.
     */
    PRINTER_ERROR(
        "A252",
        ProcessingErrorEvent.PRINTER_ERROR
    ),

    /**
     * Low battery for printing.
     * Connect charger and try again.
     */
    LOW_BATTERY_FOR_PRINTING(
        "A254",
        ProcessingErrorEvent.PRINT_ERROR_LOW_BATTERY
    ),

    /**
     * Request cannot be completed.
     * Try again later.
     */
    REQUEST_CANNOT_BE_COMPLETED(
        "M1000",
        ProcessingErrorEvent.REQUEST_CANNOT_BE_COMPLETED
    ),

    /**
     * Request cannot be executed.
     * Try again later.
     */
    REQUEST_CANNOT_BE_EXECUTED(
        "M1003",
        ProcessingErrorEvent.REQUEST_CANNOT_BE_EXECUTED
    ),

    /**
     * PagSeguro account closed.
     * Contact PagSeguro.
     */
    ACCOUNT_CLOSED(
        "M2042",
        ProcessingErrorEvent.ACCOUNT_CLOSED
    ),

    /**
     * App update required.
     * Update application to continue.
     */
    APP_UPDATE_REQUIRED(
        "M2054",
        ProcessingErrorEvent.APP_UPDATE
    ),

    /**
     * Operation not completed.
     * Try again later.
     */
    OPERATION_NOT_COMPLETED(
        "M2060",
        ProcessingErrorEvent.OPERATION_NOT_COMPLETED
    ),

    /**
     * Terminal update required.
     * Update terminal to continue selling.
     */
    TERMINAL_UPDATE_REQUIRED(
        "M2069",
        ProcessingErrorEvent.TERMINAL_UPDATE_REQUIRED
    ),

    /**
     * Generic retry error.
     * Try again.
     */
    GENERIC_RETRY_ERROR(
        "M3017",
        ProcessingErrorEvent.GENERIC
    ),

    /**
     * Authentication required.
     * Re-login to the application.
     */
    AUTHENTICATION_REQUIRED(
        "M3021",
        ProcessingErrorEvent.AUTHENTICATION_REQUIRED
    ),

    /**
     * Terminal not configured.
     * Configure terminal parameters.
     */
    TERMINAL_NOT_CONFIGURED_A99_ALT1(
        "A99",
        ProcessingErrorEvent.TERMINAL_NOT_CONFIGURED
    ),

    /**
     * Initialization error.
     * Restart terminal and try again.
     */
    INITIALIZATION_ERROR_A99_ALT2(
        "A99",
        ProcessingErrorEvent.INITIALIZATION_ERROR
    ),

    /**
     * Terminal not configured.
     * Configure terminal parameters.
     */
    TERMINAL_NOT_CONFIGURED_A99_ALT3(
        "A99",
        ProcessingErrorEvent.TERMINAL_NOT_CONFIGURED
    ),

    /**
     * App update required.
     * Update application to continue.
     */
    APP_UPDATE_REQUIRED_A99_ALT4(
        "A99",
        ProcessingErrorEvent.APP_UPDATE
    ),

    /**
     * Printer error.
     * Contact support for assistance.
     */
    PRINTER_ERROR_A99_ALT5(
        "A99",
        ProcessingErrorEvent.PRINTER_ERROR
    ),

    /**
     * Operation not completed.
     * Try again later.
     */
    OPERATION_NOT_COMPLETED_A206(
        "A206",
        ProcessingErrorEvent.OPERATION_NOT_COMPLETED
    ),

    /**
     * Operation canceled by user.
     * User pressed cancel button.
     */
    USER_CANCELED_OPERATION_A242(
        "A242",
        ProcessingErrorEvent.CANCELLED_BY_USER
    ),

    /**
     * Initialization error.
     * Restart terminal and try again.
     */
    INITIALIZATION_ERROR_A243(
        "A243",
        ProcessingErrorEvent.INITIALIZATION_ERROR
    ),

    /**
     * Initialization error.
     * Restart terminal and try again.
     */
    INITIALIZATION_ERROR_A244(
        "A244",
        ProcessingErrorEvent.INITIALIZATION_ERROR
    ),

    /**
     * Initialization error.
     * Restart terminal and try again.
     */
    INITIALIZATION_ERROR_A245(
        "A245",
        ProcessingErrorEvent.INITIALIZATION_ERROR
    ),

    /**
     * Generic retry error with specific message.
     * Try again later.
     */
    GENERIC_RETRY_WITH_MESSAGE_M3017(
        "M3017",
        ProcessingErrorEvent.GENERIC_RETRY
    ),
    
    /**
     * Message buffer overflow.
     * Contact support with logs.
     */
    MESSAGE_BUFFER_OVERFLOW(
        "-1001",
        ProcessingErrorEvent.MESSAGE_BUFFER_OVERFLOW
    ),
    
    /**
     * Invalid application parameter.
     * Contact support with logs.
     */
    INVALID_APPLICATION_PARAMETER(
        "-1002",
        ProcessingErrorEvent.INVALID_APPLICATION_PARAMETER
    ),
    
    /**
     * Terminal not ready.
     * Try again.
     */
    TERMINAL_NOT_READY(
        "-1003",
        ProcessingErrorEvent.TERMINAL_NOT_CONFIGURED
    ),


    /**
     * Total transaction value parameter cannot be null.
     * Check implementation.
     */
    NULL_TOTAL_VALUE(
        "-1007",
        ProcessingErrorEvent.TRANSACTION_NULL_AMOUNT
    ),


    /**
     * Corrupted reception buffer.
     * Retry transaction.
     */
    CORRUPTED_RECEPTION_BUFFER(
        "-1014",
        ProcessingErrorEvent.CORRUPTED_RECEPTION_BUFFER
    ),
    
    /**
     * Application name too long.
     * Limit application name to 25 characters.
     */
    APPLICATION_NAME_TOO_LONG(
        "-1015",
        ProcessingErrorEvent.APPLICATION_NAME_TOO_LONG
    ),
    
    /**
     * Application version too long.
     * Limit application version to 10 characters.
     */
    APPLICATION_VERSION_TOO_LONG(
        "-1016",
        ProcessingErrorEvent.APPLICATION_VERSION_TOO_LONG
    ),
    
    /**
     * Application name required.
     * Set name and version with setVersionName(String, String).
     */
    APPLICATION_NAME_REQUIRED(
        "-1017",
        ProcessingErrorEvent.APPLICATION_NAME_REQUIRED
    ),


    /**
     * Table loading error.
     * Reinitialize (reload tables).
     */
    TABLE_LOADING_ERROR(
        "-1024",
        ProcessingErrorEvent.TABLE_LOADING_ERROR
    ),
    
    /**
     * Token not found.
     * Reauthenticate.
     */
    TOKEN_NOT_FOUND(
        "-1030",
        ProcessingErrorEvent.TOKEN_NOT_FOUND
    ),


    /**
     * Authentication error.
     * Check username/password or activation code and try again.
     */
    AUTHENTICATION_ERROR_1033(
        "-1033",
        ProcessingErrorEvent.AUTHENTICATION_ERROR
    ),


    /**
     * Invalid device identification.
     * Check device registration.
     */
    INVALID_DEVICE_ID(
        "-1035",
        ProcessingErrorEvent.INVALID_DEVICE_ID
    ),


    /**
     * Invalid value.
     * Check payment value and try again.
     */
    INVALID_VALUE_1038(
        "-1038",
        ProcessingErrorEvent.TRANSACTION_INVALID_AMOUNT
    ),
    
    /**
     * Root permission detected.
     * Remove root permission from device.
     */
    ROOT_PERMISSION_DETECTED(
        "-3001",
        ProcessingErrorEvent.ROOT_PERMISSION_DETECTED
    ),
    
    /**
     * No authentication data.
     * Perform authentication.
     */
    NO_AUTHENTICATION_DATA(
        "-4046",
        ProcessingErrorEvent.NO_AUTHENTICATION_DATA
    ),
    
    /**
     * Printer out of paper.
     * Check printer paper supply.
     */
    PRINTER_NO_PAPER(
        "-5002",
        ProcessingErrorEvent.PRINTER_OUT_OF_PAPER
    ),

    /**
     * Low voltage.
     * Check device battery, below 15% may cause this error.
     */
    LOW_VOLTAGE(
        "-5004",
        ProcessingErrorEvent.LOW_BATTERY
    ),
    
    /**
     * Data packet format error.
     * Restart device and try again.
     */
    DATA_PACKET_FORMAT_ERROR(
        "-5006",
        ProcessingErrorEvent.DATA_PACKET_FORMAT
    ),
    
    /**
     * Printer malfunction.
     * Try again or check printer hardware.
     */
    PRINTER_MALFUNCTION(
        "-5007",
        ProcessingErrorEvent.PRINTER_MALFUNCTION
    ),
    
    /**
     * Printing unfinished.
     * Check printer paper and try again.
     */
    PRINTING_UNFINISHED(
        "-5008",
        ProcessingErrorEvent.PRINTING_UNFINISHED
    ),
    
    /**
     * Data package too long.
     * Check file size.
     */
    DATA_PACKAGE_TOO_LONG(
        "-5010",
        ProcessingErrorEvent.DATA_PACKAGE_TOO_LONG
    ),

    /**
     * Print file not found.
     * Try again with valid file.
     */
    PRINT_FILE_NOT_FOUND(
        "-5011",
        ProcessingErrorEvent.PRINT_FILE_NOT_FOUND
    ),

    /**
     * Print file not found.
     * Try again with valid file.
     */
    PRINT_FILE_NOT_FOUND_2(
        "I013",
        ProcessingErrorEvent.PRINT_FILE_NOT_FOUND
    ),

    /**
     * SDK not available for printing.
     * Restart device and try again.
     */
    SDK_PRINT_UNAVAILABLE(
        "-5012",
        ProcessingErrorEvent.SDK_PRINT_UNAVAILABLE
    ),
    
    /**
     * Invalid file.
     * Try again with valid file.
     */
    INVALID_FILE(
        "-5013",
        ProcessingErrorEvent.INVALID_FILE
    ),
    
    /**
     * Image processing failed.
     * Try again with different image.
     */
    IMAGE_PROCESSING_FAILED(
        "-5014",
        ProcessingErrorEvent.IMAGE_PROCESSING_FAILED
    );

    companion object {
        /**
         * Translates a PagSeguro error code to a ProcessingErrorEvent.
         * @param code The error code from PagSeguro SDK.
         * @return The corresponding ProcessingErrorEvent.
         */
        fun translate(code: String?, result: String?): ProcessingErrorEvent {
            return entries.find {
                it.code == code || it.code == result
            }?.event ?: ProcessingErrorEvent.GENERIC
        }

        /**
         * Translates a ProcessingErrorEvent to its PagSeguro corresponding error code.
         * @param event The ProcessingErrorEvent to translate.
         * @return The corresponding error code.
         */
        fun translate(event: ProcessingErrorEvent): String {
            return entries.find { it.event == event }?.code ?: "UNKNOWN"
        }
    }
}
