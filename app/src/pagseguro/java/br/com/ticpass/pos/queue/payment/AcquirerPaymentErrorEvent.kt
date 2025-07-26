package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.ProcessingErrorEvent

/**
 * PagSeguro-specific error events emitted during payment processing
 */
enum class AcquirerPaymentErrorEvent(val code: String, val event: ProcessingErrorEvent) {

    /**
     * Invalid transaction. Contact support.
     */
    INVALID_TRANSACTION_S40("S40", ProcessingErrorEvent.INVALID_TRANSACTION),
    
    /**
     * Invalid transaction. Contact support.
     */
    INVALID_TRANSACTION_S41("S41", ProcessingErrorEvent.INVALID_TRANSACTION),
    
    /**
     * Invalid MasterDebit transaction. Use debit function instead of credit.
     */
    INVALID_MASTERDEBIT_TRANSACTION_S42("S42", ProcessingErrorEvent.INVALID_TRANSACTION),
    
    /**
     * Invalid transaction.
     */
    INVALID_TRANSACTION_S44("S44", ProcessingErrorEvent.INVALID_TRANSACTION),
    
    /**
     * Unexpected error. Try again.
     */
    UNEXPECTED_ERROR_S45("S45", ProcessingErrorEvent.UNEXPECTED_ERROR),
    
    /**
     * Attempts exceeded. Use another card.
     */
    ATTEMPTS_EXCEEDED_S46("S46", ProcessingErrorEvent.ATTEMPTS_EXCEEDED),
    
    /**
     * Installment not allowed for this transaction type. Use another card or don't use installments.
     */
    INSTALLMENT_NOT_ALLOWED_S47("S47", ProcessingErrorEvent.INSTALLMENT_NOT_ALLOWED),
    
    /**
     * Installment not allowed for prepaid card.
     */
    INSTALLMENT_NOT_ALLOWED_PREPAID_S51("S51", ProcessingErrorEvent.INSTALLMENT_NOT_ALLOWED_PREPAID),
    
    /**
     * Use credit function to perform pre-authorization.
     */
    USE_CREDIT_FOR_PREAUTH_S53("S53", ProcessingErrorEvent.USE_CREDIT_FOR_PREAUTH),
    
    /**
     * Capture amount is greater than pre-authorized amount.
     */
    CAPTURE_AMOUNT_EXCEEDED_S54("S54", ProcessingErrorEvent.CAPTURE_AMOUNT_EXCEEDED),
    
    /**
     * Card brand not allowed for pre-authorization.
     */
    BRAND_NOT_ALLOWED_PREAUTH_S57("S57", ProcessingErrorEvent.BRAND_NOT_ALLOWED_PREAUTH),
    
    /**
     * Pre-authorization cannot be captured. Validity expired.
     */
    PREAUTH_EXPIRED_S58("S58", ProcessingErrorEvent.PREAUTH_EXPIRED),
    
    /**
     * Pre-authorization not enabled. Contact support.
     */
    PREAUTH_NOT_ENABLED_S62("S62", ProcessingErrorEvent.PREAUTH_NOT_ENABLED),
    
    /**
     * Card not identified. Use another card.
     */
    CARD_NOT_IDENTIFIED_S63("S63", ProcessingErrorEvent.CARD_NOT_IDENTIFIED),
    
    /**
     * Operation timeout exceeded. Try again.
     */
    OPERATION_TIMEOUT_S66("S66", ProcessingErrorEvent.OPERATION_TIMEOUT),
    
    /**
     * Operation not allowed. Invalid card data.
     */
    OPERATION_NOT_ALLOWED_S67("S67", ProcessingErrorEvent.INVALID_CARD_DATA),
    
    /**
     * Minimum installment amount is $5.00.
     */
    MINIMUM_INSTALLMENT_AMOUNT_S69("S69", ProcessingErrorEvent.MINIMUM_INSTALLMENT_AMOUNT),
    
    /**
     * Amount lower than allowed.
     */
    AMOUNT_TOO_LOW_S71("S71", ProcessingErrorEvent.AMOUNT_TOO_LOW),
    
    /**
     * Pre-authorization quantity exceeded.
     */
    PREAUTH_QUANTITY_EXCEEDED_S72("S72", ProcessingErrorEvent.PREAUTH_QUANTITY_EXCEEDED),
    
    /**
     * Operation not allowed. Request in progress.
     */
    REQUEST_IN_PROGRESS_S73("S73", ProcessingErrorEvent.REQUEST_IN_PROGRESS),

    /**
     * Feature unavailable. Cancel through website.
     */
    FUNCTION_UNAVAILABLE_PP1049("PP1049", ProcessingErrorEvent.FEATURE_UNAVAILABLE),

    /**
     * Function unavailable. Cancel through website.
     */
    FUNCTION_UNAVAILABLE_S510("S510", ProcessingErrorEvent.FEATURE_UNAVAILABLE),
    
    /**
     * Function unavailable. Cancel through website.
     */
    FUNCTION_UNAVAILABLE_S512("S512", ProcessingErrorEvent.FEATURE_UNAVAILABLE),
    
    /**
     * Transaction not authorized. Contact support.
     */
    TRANSACTION_NOT_AUTHORIZED_S899("S899", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED),
    
    /**
     * Unexpected error. Try again.
     */
    UNEXPECTED_ERROR_S906("S906", ProcessingErrorEvent.UNEXPECTED_ERROR),

    /**
     * Invalid holder.
     */
    INVALID_HOLDER_M5002("M5002", ProcessingErrorEvent.INVALID_HOLDER),
    
    /**
     * Invalid embedded reader serial number.
     */
    INVALID_READER_SERIAL_NUMBER_M5003("M5003", ProcessingErrorEvent.INVALID_READER_SERIAL_NUMBER),
    
    /**
     * Reader serial number not identified.
     */
    READER_SERIAL_NOT_IDENTIFIED_M5004("M5004", ProcessingErrorEvent.UNIDENTIFIED_READER_SERIAL_NUMBER),
    
    /**
     * Inactive reader.
     */
    INACTIVE_READER_M5005("M5005", ProcessingErrorEvent.INACTIVE_READER),
    
    /**
     * Incorrect code.
     * Check if the activation code entered is correct. Wait 30 minutes to try again.
     */
    INCORRECT_CODE_M5007("M5007", ProcessingErrorEvent.INCORRECT_ACTIVATION_CODE),
    
    /**
     * Unexpected error.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    UNEXPECTED_ERROR_S01("S01", ProcessingErrorEvent.UNEXPECTED_ERROR),
    
    /**
     * Operation time exceeded.
     * Try processing the sale again, if the problem persists, there may be some unavailability.
     */
    OPERATION_TIME_EXCEEDED_S02("S02", ProcessingErrorEvent.OPERATION_TIME_EXCEEDED),
    
    /**
     * Connection refused.
     * Try processing the sale again, if the problem persists, there may be some unavailability.
     */
    CONNECTION_REFUSED_S03("S03", ProcessingErrorEvent.CONNECTION_REFUSED),

    
    /**
     * Transaction not found.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    TRANSACTION_NOT_FOUND_S08("S08", ProcessingErrorEvent.TRANSACTION_NOT_FOUND),
    
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
     * Invalid input mode.
     * If the transaction was processed with chip authentication, the reversal must be done with chip, if it was processed with stripe, reversal with stripe.
     */
    INVALID_INPUT_MODE_S14("S14", ProcessingErrorEvent.INVALID_INPUT_MODE),
    
    /**
     * Invalid BIN-Holder.
     * Make reversal with the same card that processed the transaction.
     */
    INVALID_BIN_HOLDER_S15("S15", ProcessingErrorEvent.INVALID_BIN),
    
    /**
     * Transaction already reversed.
     */
    TRANSACTION_ALREADY_REVERSED_S16("S16", ProcessingErrorEvent.TRANSACTION_ALREADY_REVERSED),
    
    /**
     * Unexpected error.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    UNEXPECTED_ERROR_S17("S17", ProcessingErrorEvent.UNEXPECTED_ERROR),
    
    /**
     * Response TLV with size larger than expected.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    RESPONSE_TLV_SIZE_LARGER_S18("S18", ProcessingErrorEvent.RESPONSE_TLV_SIZE),
    
    /**
     * Invalid mode.
     * If the transaction is credit, check if the card has credit mode enabled, same case for debit.
     */
    INVALID_MODE_S19("S19", ProcessingErrorEvent.INVALID_PAYMENT_METHOD),
    
    /**
     * Duplicate transaction.
     * If the intention is really to process 2x the same value on the same card, the value should be changed.
     */
    DUPLICATE_TRANSACTION_S20("S20", ProcessingErrorEvent.DUPLICATE_TRANSACTION),
    
    /**
     * Operation not performed. Enter password for this operation.
     * Enter the password.
     */
    OPERATION_NOT_PERFORMED_ENTER_PASSWORD_S21("S21", ProcessingErrorEvent.OPERATION_NOT_PERFORMED_ENTER_PASSWORD),
    
    /**
     * Use CHIP for this transaction.
     * Perform the reversal using the chip.
     */
    USE_CHIP_FOR_TRANSACTION_S22("S22", ProcessingErrorEvent.USE_CHIP_FOR_TRANSACTION),
    
    /**
     * Error opening cryptogram.
     * Send for analysis the seller's email, date, time and terminal number.
     */
    ERROR_OPENING_CRYPTOGRAM_S24("S24", ProcessingErrorEvent.ERROR_OPENING_CRYPTOGRAM),
    
    /**
     * Error confirming transaction.
     * Try processing the sale again.
     */
    ERROR_CONFIRMING_TRANSACTION_S25("S25", ProcessingErrorEvent.ERROR_CONFIRMING_TRANSACTION),
    
    /**
     * Terminal not found.
     * Check terminal registration in the system.
     */
    TERMINAL_NOT_FOUND_S26("S26", ProcessingErrorEvent.TERMINAL_NOT_FOUND),
    
    /**
     * Product not enabled.
     * Customer must register with the desired voucher brand.
     */
    PRODUCT_NOT_ENABLED_S28("S28", ProcessingErrorEvent.PRODUCT_NOT_ENABLED),
    
    /**
     * Failed to generate MAC.
     * Contact support.
     */
    FAILED_TO_GENERATE_MAC_S29("S29", ProcessingErrorEvent.MAC_GENERATION_FAIL),
    
    /**
     * Only total reversal allowed.
     * Perform total reversal.
     */
    ONLY_TOTAL_REVERSAL_ALLOWED_S30("S30", ProcessingErrorEvent.ONLY_TOTAL_REVERSAL_ALLOWED),
    
    /**
     * Error in response message validation.
     * Contact support.
     */
    ERROR_RESPONSE_MESSAGE_VALIDATION_S31("S31", ProcessingErrorEvent.ERROR_RESPONSE_MESSAGE_VALIDATION),
    
    /**
     * Invalid POS key.
     * Contact support.
     */
    INVALID_POS_KEY_S33("S33", ProcessingErrorEvent.INVALID_POS_KEY),
    
    /**
     * Invalid transaction.
     * Perform the sale in cash.
     */
    INVALID_TRANSACTION_S39("S39", ProcessingErrorEvent.INVALID_TRANSACTION_PERFORM_CASH),
    

    /**
     * Invalid installment value.
     * The installment value must be equal to or greater than R$5.00.
     */
    INVALID_INSTALLMENT_VALUE_M2006("M2006", ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT),
    
    /**
     * Invalid count of installments.
     */
    INVALID_NUMBER_OF_INSTALLMENTS_M2007("M2007", ProcessingErrorEvent.INVALID_INSTALLMENTS_COUNT),
    
    /**
     * Invalid installment type.
     */
    INVALID_INSTALLMENT_TYPE_M2010("M2010", ProcessingErrorEvent.INVALID_INSTALLMENT_METHOD),
    
    /**
     * Error reversing transaction.
     * Please try again later.
     */
    ERROR_REVERSING_TRANSACTION_M2017("M2017", ProcessingErrorEvent.ERROR_REVERSING_TRANSACTION),
    
    /**
     * Invalid value for installment.
     */
    INVALID_VALUE_FOR_INSTALLMENT_M2018("M2018", ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT),
    
    /**
     * Invalid card number.
     */
    INVALID_CARD_NUMBER_M2019("M2019", ProcessingErrorEvent.INVALID_CARD_NUMBER),
    
    /**
     * Transaction value must be equal to or greater than R$1.00.
     */
    TRANSACTION_VALUE_TOO_LOW_M2020("M2020", ProcessingErrorEvent.MINIMUM_INSTALLMENT_AMOUNT),
    
    /**
     * Could not identify the card.
     */
    COULD_NOT_IDENTIFY_CARD_M2022("M2022", ProcessingErrorEvent.COULD_NOT_IDENTIFY_CARD),
    
    /**
     * Transaction not authorized by card issuer.
     */
    TRANSACTION_NOT_AUTHORIZED_BY_ISSUER_M2035("M2035", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_ISSUER),
    
    /**
     * Invalid card data.
     */
    INVALID_CARD_DATA_M2036("M2036", ProcessingErrorEvent.INVALID_CARD_DATA),
    
    /**
     * Account has been closed.
     * Contact support.
     */
    ACCOUNT_CLOSED_M2042("M2042", ProcessingErrorEvent.ACCOUNT_CLOSED),
    
    /**
     * Please check your password.
     */
    CHECK_PASSWORD_M2048("M2048", ProcessingErrorEvent.CHECK_PASSWORD),
    
    /**
     * Please enter a maximum of 10 characters.
     * The userReference field only accepts up to 10 characters.
     */
    MAX_10_CHARACTERS_M2049("M2049", ProcessingErrorEvent.MAX_CHARACTERS),
    
    /**
     * Please enter only letters and numbers.
     * The userReference field only accepts letters and numbers.
     */
    ONLY_LETTERS_AND_NUMBERS_M2050("M2050", ProcessingErrorEvent.ONLY_LETTERS_AND_NUMBERS),
    
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
    UPDATE_DEVICE_M2069("M2069", ProcessingErrorEvent.UPDATE_DEVICE),
    
    /**
     * Transaction not found.
     * The transaction may have already been reversed.
     */
    TRANSACTION_NOT_FOUND_M3004("M3004", ProcessingErrorEvent.TRANSACTION_NOT_FOUND),
    
    /**
     * Maximum date for transaction reversal has been exceeded.
     */
    MAX_DATE_FOR_REVERSAL_EXCEEDED_M3005("M3005", ProcessingErrorEvent.MAX_DATE_FOR_REVERSAL_EXCEEDED),
    
    /**
     * Transaction cannot be reversed.
     */
    TRANSACTION_CANNOT_BE_REVERSED_M3006("M3006", ProcessingErrorEvent.TRANSACTION_CANNOT_BE_REVERSED),
    
    /**
     * Transaction cannot be reversed.
     */
    TRANSACTION_CANNOT_BE_REVERSED_M3008("M3008", ProcessingErrorEvent.TRANSACTION_CANNOT_BE_REVERSED),
    
    /**
     * Transaction cannot be confirmed.
     */
    TRANSACTION_CANNOT_BE_CONFIRMED_M3009("M3009", ProcessingErrorEvent.TRANSACTION_CANNOT_BE_CONFIRMED),
    
    /**
     * Transaction cannot be cancelled.
     */
    TRANSACTION_CANNOT_BE_CANCELLED_M3010("M3010", ProcessingErrorEvent.TRANSACTION_CANNOT_BE_CANCELLED),
    
    /**
     * Transaction not authorized.
     */
    TRANSACTION_NOT_AUTHORIZED_M3011("M3011", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED),
    
    /**
     * Transaction not authorized.
     * Contact your commercial manager/risk area.
     */
    TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER_M3012("M3012", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER),
    
    /**
     * Seller blocked.
     * Contact your commercial manager.
     */
    SELLER_BLOCKED_M3013("M3013", ProcessingErrorEvent.SELLER_BLOCKED),
    
    /**
     * Email not confirmed.
     * Please verify your email.
     */
    EMAIL_NOT_CONFIRMED_M3014("M3014", ProcessingErrorEvent.EMAIL_NOT_CONFIRMED),
    
    /**
     * Try again.
     */
    TRY_AGAIN_M3017("M3017", ProcessingErrorEvent.TRY_AGAIN),
    
    /**
     * Cannot make sale with international card.
     */
    CANNOT_SALE_INTERNATIONAL_CARD_M3020("M3020", ProcessingErrorEvent.INTERNATIONAL_CARD_NOT_ALLOWED),
    
    /**
     * Please log in to the app again.
     * Redo the terminal activation.
     */
    PLEASE_LOGIN_AGAIN_M3021("M3021", ProcessingErrorEvent.PLEASE_LOGIN_AGAIN),
    
    /**
     * Could not configure installment.
     */
    COULD_NOT_CONFIGURE_INSTALLMENT_M3022("M3022", ProcessingErrorEvent.COULD_NOT_CONFIGURE_INSTALLMENT),
    
    /**
     * Could not locate, the reference is duplicated.
     */
    COULD_NOT_LOCATE_REFERENCE_DUPLICATED_M3023("M3023", ProcessingErrorEvent.COULD_NOT_LOCATE_REFERENCE_DUPLICATED),
    
    /**
     * Transaction not authorized.
     * Contact your commercial manager/risk area.
     */
    TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER_M3024("M3024", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED),
    
    /**
     * Invalid reader or activation code.
     * Check if the activation code is correct.
     */
    INVALID_READER_OR_ACTIVATION_CODE_M5000("M5000", ProcessingErrorEvent.INVALID_READER_OR_ACTIVATION_CODE),
    
    /**
     * Invalid BIN.
     */
    INVALID_BIN_M5001("M5001", ProcessingErrorEvent.INVALID_BIN),

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
     * Read error, try again.
     * Try again.
     */
    READ_ERROR_B19("B19", ProcessingErrorEvent.READ_ERROR),
    
    /**
     * Transaction denied.
     * Contact the issuing bank.
     */
    TRANSACTION_DENIED_B24("B24", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_ISSUER),
    
    /**
     * Response time exceeded.
     * Try again.
     */
    RESPONSE_TIME_EXCEEDED_B25("B025", ProcessingErrorEvent.RESPONSE_TIME_EXCEEDED),
    
    /**
     * Communication problem, try again.
     * Check communication (Chip or wifi).
     */
    COMMUNICATION_PROBLEM_B26("B26", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Use chip for this transaction.
     * Make the transaction with card chip.
     */
    USE_CHIP_INVALID_PAYMENT_B29("B29", ProcessingErrorEvent.USE_CHIP_INVALID_PAYMENT),
    
    /**
     * Invalid payment method.
     * Check if the card has the chosen payment method enabled.
     */
    INVALID_MODE_B37("B37", ProcessingErrorEvent.INVALID_PAYMENT_METHOD),
    
    /**
     * Operation not performed.
     * Check if the card has the chosen payment method enabled.
     */
    OPERATION_NOT_PERFORMED_B41("B41", ProcessingErrorEvent.OPERATION_NOT_PERFORMED),
    
    /**
     * Communication problem, try again.
     * Try again.
     */
    COMMUNICATION_PROBLEM_B45("B45", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Card with error or poorly inserted.
     * Try again.
     */
    CARD_ERROR_POORLY_INSERTED_B51("B51", ProcessingErrorEvent.CARD_ERROR_POORLY_INSERTED),
    
    /**
     * Pinpad error.
     * Equipment should be replaced.
     */
    PINPAD_ERROR_C10("C10", ProcessingErrorEvent.PINPAD_ERROR),
    
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
     * Internal pinpad error.
     * Equipment should be replaced.
     */
    INTERNAL_PINPAD_ERROR_C40("C40", ProcessingErrorEvent.INTERNAL_PINPAD_ERROR),
    
    /**
     * Reader without required key.
     * Force new table loading.
     */
    READER_WITHOUT_KEY_C42("C42", ProcessingErrorEvent.READER_WITHOUT_KEY),
    
    /**
     * Card with error or poorly inserted.
     * Try again, if it doesn't work, use another card.
     */
    CARD_ERROR_POORLY_INSERTED_C60("C60", ProcessingErrorEvent.CARD_ERROR_POORLY_INSERTED),
    
    /**
     * Card with error or poorly inserted.
     * Try again, if it doesn't work, use another card.
     */
    CARD_ERROR_POORLY_INSERTED_C61("C61", ProcessingErrorEvent.CARD_ERROR_POORLY_INSERTED),
    
    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    INVALID_CARD_C62("C62", ProcessingErrorEvent.INVALID_CARD),
    
    /**
     * Blocked card.
     * Card is blocked. Unblock or use another card.
     */
    BLOCKED_CARD_C63("C63", ProcessingErrorEvent.BLOCKED_CARD),
    
    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    INVALID_CARD_C64("C64", ProcessingErrorEvent.INVALID_CARD_NOT_ACCEPTED),
    
    /**
     * Expired card.
     * Use another card.
     */
    EXPIRED_CARD_C65("C65", ProcessingErrorEvent.EXPIRED_CARD),
    
    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    INVALID_CARD_C66("C66", ProcessingErrorEvent.INVALID_CARD_USE_ANOTHER),
    
    /**
     * Card invalidated.
     * Card is blocked. Contact the issuing bank or use another card.
     */
    CARD_INVALIDATED_C67("C67", ProcessingErrorEvent.CARD_INVALIDATED),
    
    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    INVALID_CARD_C68("C68", ProcessingErrorEvent.INVALID_CARD_NOT_ACCEPTED),
    
    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    INVALID_CARD_C69("C69", ProcessingErrorEvent.INVALID_CARD_NOT_ACCEPTED),
    
    /**
     * Invalid payment method.
     * A method that the card does not have may have been selected (debit, credit or financing).
     */
    INVALID_MODE_C70("C70", ProcessingErrorEvent.INVALID_PAYMENT_METHOD),
    
    /**
     * Card brand not accepted.
     * Use a card with one of the accepted brands.
     */
    CARD_NOT_ACCEPTED_C71("C71", ProcessingErrorEvent.CARD_BRAND_NOT_ACCEPTED),
    
    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    INVALID_CARD_C76("C76", ProcessingErrorEvent.CARD_BRAND_NOT_ACCEPTED),
    
    /**
     * Invalid card, use Chip/stripe.
     * Try again.
     */
    INVALID_CARD_USE_CHIP_C83("C83", ProcessingErrorEvent.USE_CHIP_FOR_TRANSACTION),

    /**
     * Invalid selected option.
     * Contactless product different from the one selected for payment.
     */
    INVALID_SELECTED_OPTION_C84("C84", ProcessingErrorEvent.INVALID_SELECTED_OPTION),

    /**
     * Invalid selected option.
     * Contactless product different from the one selected for payment.
     */
    CARD_REACH_NOT_ALLOWED("C87", ProcessingErrorEvent.CARD_REACH_NOT_ALLOWED),
    
    /**
     * Not authorized.
     * Try again, if it persists contact your commercial manager.
     */
    NOT_AUTHORIZED_M826("M826", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED),
    
    /**
     * Contactless not authorized, insert card.
     * Try again, if it persists contact your commercial manager.
     */
    CONTACTLESS_NOT_AUTHORIZED_M831("M831", ProcessingErrorEvent.CONTACTLESS_NOT_AUTHORIZED),

    /**
     * Request cannot be completed.
     * Please try again later.
     */
    REQUEST_CANNOT_BE_COMPLETED_M1000("M1000", ProcessingErrorEvent.REQUEST_CANNOT_BE_COMPLETED),

    /**
     * Request cannot be completed 2.
     * Please try again later.
     */
    REQUEST_CANNOT_BE_COMPLETED_M1000_2("-1000", ProcessingErrorEvent.REQUEST_CANNOT_BE_COMPLETED),
    
    /**
     * Request cannot be executed.
     * Please try again later.
     */
    REQUEST_CANNOT_BE_EXECUTED_M1003("M1003", ProcessingErrorEvent.REQUEST_CANNOT_BE_EXECUTED),
    
    /**
     * Communication error.
     * Try again.
     */
    COMMUNICATION_ERROR_A220("A220", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication error.
     * Try again.
     */
    COMMUNICATION_ERROR_A221("A221", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication error.
     * Try again.
     */
    COMMUNICATION_ERROR_A222("A222", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication timeout.
     * Try again.
     */
    COMMUNICATION_TIMEOUT_A223("A223", ProcessingErrorEvent.COMMUNICATION_TIMEOUT),
    
    /**
     * Communication error.
     * Try again.
     */
    COMMUNICATION_ERROR_A224("A224", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A225("A225", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A226("A226", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A227("A227", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A228("A228", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A229("A229", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A230("A230", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A231("A231", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A232("A232", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A234("A234", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A235("A235", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A236("A236", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A237("A237", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A238("A238", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
    /**
     * Invalid menu option.
     * Internal application error.
     */
    INVALID_MENU_OPTION_A239("A239", ProcessingErrorEvent.INVALID_MENU_OPTION),
    
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
     * WiFi authentication error.
     * Check connection.
     */
    WIFI_AUTH_ERROR_A246("A246", ProcessingErrorEvent.WIFI_AUTH_ERROR),
    
    /**
     * WiFi networks not found.
     * Try again.
     */
    WIFI_NETWORKS_NOT_FOUND_A247("A247", ProcessingErrorEvent.WIFI_NETWORKS_NOT_FOUND),
    
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
     * Transaction confirmation in progress.
     * Try again later or send receipt via email/SMS.
     */
    TRANSACTION_CONFIRMATION_IN_PROGRESS_A255("A255", ProcessingErrorEvent.TRANSACTION_CONFIRMATION_IN_PROGRESS),
    
    /**
     * Print error file not found.
     * File for printing not found.
     */
    PRINT_ERROR_FILE_NOT_FOUND_A256("A256", ProcessingErrorEvent.PRINT_FILE_NOT_FOUND),
    
    /**
     * Communication problem.
     * Try again.
     */
    COMMUNICATION_PROBLEM_A306("A306", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Transaction failure.
     * Try again.
     */
    TRANSACTION_FAILURE_A307("A307", ProcessingErrorEvent.TRANSACTION_FAILURE),
    
    /**
     * Mobile communication problem.
     * Communication problem with mobile.
     */
    MOBILE_COMMUNICATION_PROBLEM_A9404("A9404", ProcessingErrorEvent.MOBILE_COMMUNICATION_ERROR),
    
    /**
     * Print error due to low battery.
     * Connect charger and try again.
     */
    PRINT_ERROR_LOW_BATTERY_A52("A52", ProcessingErrorEvent.PRINT_ERROR_LOW_BATTERY),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A53("A53", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A54("A54", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A55("A55", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A56("A56", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A57("A57", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication problem.
     * Try again.
     */
    COMMUNICATION_PROBLEM_A58("A58", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication problem.
     * Wait a few minutes and try again.
     */
    COMMUNICATION_PROBLEM_WAIT_A59("A59", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication problem.
     * Generic communication error.
     */
    COMMUNICATION_PROBLEM_A102("A102", ProcessingErrorEvent.COMMUNICATION_ERROR),
    /**
     * Invalid parameter.
     * Internal application error.
     */
    INVALID_PARAMETER_A201("A201", ProcessingErrorEvent.INVALID_PARAMETER),
    
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
     * Transaction denied.
     * Retry transaction.
     */
    TRANSACTION_DENIED_A209("A209", ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A214("A214", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A215("A215", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication error.
     * Try again.
     */
    COMMUNICATION_ERROR_A219("A219", ProcessingErrorEvent.COMMUNICATION_ERROR),

    /**
     * Server operation failure.
     * Try again.
     */
    SERVER_OPERATION_FAILURE_A233("A233", ProcessingErrorEvent.ACQUIRER_SERVER_ERROR),
    
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
     * Invalid card.
     * Card validation error.
     */
    INVALID_CARD_A08("A08", ProcessingErrorEvent.INVALID_CARD),
    
    /**
     * Card operation failed.
     * Unexpected error by terminal.
     */
    CARD_OPERATION_FAILED_A09("A09", ProcessingErrorEvent.CARD_OPERATION_FAILED),
    
    /**
     * Network operator unavailable.
     * Try again.
     */
    NETWORK_OPERATOR_UNAVAILABLE_A10("A10", ProcessingErrorEvent.NETWORK_OPERATOR_UNAVAILABLE),
    
    /**
     * Network operator timeout.
     * Try again.
     */
    NETWORK_OPERATOR_TIMEOUT_A11("A11", ProcessingErrorEvent.NETWORK_OPERATOR_TIMEOUT),
    
    /**
     * Transaction failure.
     * Try again.
     */
    TRANSACTION_FAILURE_A12("A12", ProcessingErrorEvent.TRANSACTION_FAILURE),
    
    /**
     * SIM card error.
     * Contact support center.
     */
    SIM_CARD_ERROR_A14("A14", ProcessingErrorEvent.SIM_CARD_ERROR),
    
    /**
     * SIM card missing.
     * Contact support center.
     */
    SIM_CARD_MISSING_A15("A15", ProcessingErrorEvent.SIM_CARD_MISSING),
    
    /**
     * No network signal.
     * Try again.
     */
    NO_NETWORK_SIGNAL_A16("A16", ProcessingErrorEvent.NO_NETWORK_SIGNAL),
    
    /**
     * Communication problem.
     * Contact support.
     */
    COMMUNICATION_PROBLEM_A17("A17", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * PPP authentication failure.
     * Contact support center.
     */
    PPP_AUTH_FAILURE_A18("A18", ProcessingErrorEvent.PPP_AUTH_FAILURE),
    
    /**
     * Communication problem.
     * Try again.
     */
    COMMUNICATION_PROBLEM_A19("A19", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
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
     * WiFi network unavailable.
     * Try again.
     */
    WIFI_NETWORK_UNAVAILABLE_A23("A23", ProcessingErrorEvent.WIFI_NETWORK_UNAVAILABLE),
    
    /**
     * Not connected to WiFi network.
     * Configure WiFi.
     */
    NOT_CONNECTED_TO_WIFI_A24("A24", ProcessingErrorEvent.NOT_CONNECTED_TO_WIFI),
    
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
     * SIM card not responding.
     * Please wait.
     */
    SIM_CARD_NOT_RESPONDING_A28("A28", ProcessingErrorEvent.SIM_CARD_NOT_RESPONDING),
    
    /**
     * WiFi authentication error.
     * Check your WiFi connection.
     */
    WIFI_AUTH_ERROR_A29("A29", ProcessingErrorEvent.WIFI_AUTH_ERROR),
    
    /**
     * Connection error.
     * Try again.
     */
    CONNECTION_ERROR_A30("A30", ProcessingErrorEvent.CONNECTION_ERROR),
    
    /**
     * Connection error.
     * Check internet connection.
     */
    CONNECTION_ERROR_NO_INTERNET_A31("A31", ProcessingErrorEvent.CONNECTION_ERROR_NO_INTERNET),
    
    /**
     * WiFi authentication error.
     * Check your WiFi connection.
     */
    WIFI_AUTH_ERROR_A32("A32", ProcessingErrorEvent.WIFI_AUTH_ERROR),
    
    /**
     * WiFi connection error.
     * Check your connection.
     */
    WIFI_CONNECTION_ERROR_A33("A33", ProcessingErrorEvent.WIFI_CONNECTION_ERROR),
    
    /**
     * Connection error.
     * Check SIM card and WiFi network.
     */
    CONNECTION_ERROR_SIM_WIFI_A34("A34", ProcessingErrorEvent.CONNECTION_ERROR_SIM_WIFI),

    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A43("A43", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Communication failure.
     * Try again.
     */
    COMMUNICATION_FAILURE_A44("A44", ProcessingErrorEvent.COMMUNICATION_ERROR),
    
    /**
     * Transaction failure.
     * Try again.
     */
    TRANSACTION_FAILURE_A45("A45", ProcessingErrorEvent.TRANSACTION_FAILURE),

    /**
     * Transaction failure.
     * Try again.
     */
    TRANSACTION_FAILURE_A48("A48", ProcessingErrorEvent.TRANSACTION_FAILURE),
    
    /**
     * Transaction failure.
     * Try again.
     */
    TRANSACTION_FAILURE_A49("A49", ProcessingErrorEvent.TRANSACTION_FAILURE),
    
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
     * Connection refused.
     * Try processing the transaction again.
     */
    CONNECTION_REFUSED(
        "S03",
        ProcessingErrorEvent.CONNECTION_REFUSED
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
     * Transaction not found.
     * Contact support with transaction details.
     */
    TRANSACTION_NOT_FOUND(
        "S05",
        ProcessingErrorEvent.TRANSACTION_NOT_FOUND
    ),

    /**
     * Invalid entry mode.
     * Use same entry method as original transaction.
     */
    INVALID_ENTRY_MODE(
        "S06",
        ProcessingErrorEvent.INVALID_ENTRY_MODE
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
     * Invalid Bin-Holder.
     * Use same card as original transaction.
     */
    INVALID_BIN_HOLDER(
        "S08",
        ProcessingErrorEvent.INVALID_BIN
    ),

    /**
     * Transaction already refunded.
     * No action needed, refund already processed.
     */
    TRANSACTION_ALREADY_REFUNDED(
        "S09",
        ProcessingErrorEvent.TRANSACTION_ALREADY_REFUNDED
    ),

    /**
     * TLV response with larger than expected size.
     * Contact support with transaction details.
     */
    TLV_RESPONSE_TOO_LARGE(
        "S10",
        ProcessingErrorEvent.TLV_RESPONSE_TOO_LARGE
    ),

    /**
     * Invalid mode.
     * Verify card supports the selected payment mode.
     */
    INVALID_MODE(
        "S11",
        ProcessingErrorEvent.INVALID_PAYMENT_METHOD
    ),

    /**
     * Duplicate transaction.
     * Modify amount for duplicate transactions.
     */
    DUPLICATE_TRANSACTION(
        "S12",
        ProcessingErrorEvent.DUPLICATE_TRANSACTION
    ),

    /**
     * Operation not performed. Enter password for this operation.
     * Password required to complete operation.
     */
    PASSWORD_REQUIRED(
        "S13",
        ProcessingErrorEvent.PASSWORD_REQUIRED
    ),

    /**
     * Use CHIP for this transaction.
     * Process using chip authentication method.
     */
    CHIP_REQUIRED(
        "S14",
        ProcessingErrorEvent.CHIP_REQUIRED
    ),

    /**
     * Error opening cryptogram.
     * Contact support with transaction details.
     */
    CRYPTOGRAM_ERROR(
        "S15",
        ProcessingErrorEvent.ERROR_OPENING_CRYPTOGRAM
    ),

    /**
     * Error confirming transaction.
     * Try processing the transaction again.
     */
    TRANSACTION_CONFIRMATION_ERROR(
        "S16",
        ProcessingErrorEvent.TRANSACTION_CONFIRMATION_ERROR
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
     * Product not enabled.
     * Register with required payment provider.
     */
    PRODUCT_NOT_ENABLED(
        "S28",
        ProcessingErrorEvent.PRODUCT_NOT_ENABLED
    ),

    /**
     * Failed to generate MAC.
     * Contact technical support.
     */
    MAC_GENERATION_FAILED(
        "S29",
        ProcessingErrorEvent.MAC_GENERATION_FAIL
    ),

    /**
     * Only Total Refund Allowed.
     * Perform full refund instead of partial.
     */
    ONLY_TOTAL_REFUND_ALLOWED(
        "S30",
        ProcessingErrorEvent.ONLY_TOTAL_REFUND_ALLOWED
    ),

    /**
     * Error validating Response Message.
     * Contact technical support.
     */
    RESPONSE_VALIDATION_ERROR(
        "S31",
        ProcessingErrorEvent.ERROR_RESPONSE_MESSAGE_VALIDATION
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
     * Invalid Transaction.
     * Contact technical support.
     */
    INVALID_TRANSACTION_S38(
        "S38",
        ProcessingErrorEvent.INVALID_TRANSACTION
    ),

    /**
     * Invalid Transaction - Cash Only.
     * Process as cash payment only.
     */
    CASH_ONLY_TRANSACTION(
        "S39",
        ProcessingErrorEvent.CASH_ONLY_TRANSACTION
    ),

    /**
     * Invalid Card Transaction.
     * Use debit function instead of credit.
     */
    INVALID_CARD_TRANSACTION(
        "S42",
        ProcessingErrorEvent.INVALID_TRANSACTION
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
     * Transaction not authorized.
     * Verify with account management team.
     */
    TRANSACTION_NOT_AUTHORIZED_S99(
        "S99",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
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
     * Transaction denied by host.
     * Try processing the transaction again.
     */
    TRANSACTION_DENIED_BY_HOST(
        "A04",
        ProcessingErrorEvent.OPERATION_NOT_AUTHORIZED_BY_HOST
    ),

    /**
     * Invalid card.
     * Verify card type and payment method.
     */
    INVALID_CARD(
        "A08",
        ProcessingErrorEvent.INVALID_CARD
    ),

    /**
     * Operation rejected by card.
     * Try with a different card.
     */
    OPERATION_REJECTED_BY_CARD(
        "A09",
        ProcessingErrorEvent.OPERATION_REJECTED_BY_CARD
    ),

    /**
     * Telecommunication provider unavailable.
     * Try processing the transaction again.
     */
    TELECOM_PROVIDER_UNAVAILABLE(
        "A10",
        ProcessingErrorEvent.TELECOM_PROVIDER_UNAVAILABLE
    ),

    /**
     * Host response timeout.
     * Try processing the transaction again.
     */
    HOST_RESPONSE_TIMEOUT(
        "A11",
        ProcessingErrorEvent.HOST_RESPONSE_TIMEOUT
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE(
        "A12",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * SIM card error.
     * Contact support for assistance.
     */
    SIM_CARD_ERROR(
        "A14",
        ProcessingErrorEvent.SIM_CARD_ERROR
    ),

    /**
     * SIM card missing.
     * Insert SIM card and try again.
     */
    SIM_CARD_MISSING(
        "A15",
        ProcessingErrorEvent.SIM_CARD_MISSING
    ),

    /**
     * No GSM signal.
     * Move to an area with better reception.
     */
    NO_GSM_SIGNAL(
        "A16",
        ProcessingErrorEvent.NO_GSM_SIGNAL
    ),

    /**
     * Network attachment error.
     * Contact support for assistance.
     */
    NETWORK_ATTACHMENT_ERROR(
        "A17",
        ProcessingErrorEvent.NETWORK_ATTACHMENT_ERROR
    ),

    /**
     * GPRS connection error.
     * Check network settings and try again.
     */
    GPRS_CONNECTION_ERROR(
        "A18",
        ProcessingErrorEvent.GPRS_CONNECTION_ERROR
    ),

    /**
     * Socket connection error.
     * Try processing the transaction again.
     */
    SOCKET_CONNECTION_ERROR(
        "A19",
        ProcessingErrorEvent.SOCKET_CONNECTION_ERROR
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
     * Operation not authorized by host.
     * Contact support with transaction details.
     */
    OPERATION_NOT_AUTHORIZED_BY_HOST(
        "A22",
        ProcessingErrorEvent.OPERATION_NOT_AUTHORIZED_BY_HOST
    ),

    /**
     * WiFi network unavailable.
     * Check WiFi connection and try again.
     */
    WIFI_UNAVAILABLE(
        "A23",
        ProcessingErrorEvent.WIFI_NETWORK_UNAVAILABLE
    ),

    /**
     * WiFi not connected.
     * Configure WiFi connection.
     */
    WIFI_NOT_CONNECTED(
        "A24",
        ProcessingErrorEvent.WIFI_NOT_CONNECTED
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
     * Modem initialization pending.
     * Wait for modem to initialize.
     */
    MODEM_INITIALIZATION_PENDING(
        "A28",
        ProcessingErrorEvent.MODEM_INITIALIZATION_PENDING
    ),

    /**
     * WiFi authentication error.
     * Check WiFi credentials and try again.
     */
    WIFI_AUTHENTICATION_ERROR(
        "A29",
        ProcessingErrorEvent.WIFI_AUTH_ERROR
    ),

    /**
     * Connection error with server.
     * Try processing the transaction again.
     */
    SERVER_CONNECTION_ERROR(
        "A30",
        ProcessingErrorEvent.ACQUIRER_SERVER_CONNECTION_ERROR
    ),

    /**
     * Internet connection error.
     * Check internet connection and try again.
     */
    INTERNET_CONNECTION_ERROR(
        "A31",
        ProcessingErrorEvent.INTERNET_CONNECTION_ERROR
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
     * Communication interrupted.
     * Try processing the transaction again.
     */
    COMMUNICATION_INTERRUPTED(
        "A53",
        ProcessingErrorEvent.COMMUNICATION_ERROR
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
     * Generic communication error.
     * Try processing the transaction again.
     */
    GENERIC_COMMUNICATION_ERROR(
        "A102",
        ProcessingErrorEvent.COMMUNICATION_ERROR
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
     * Transaction denied by server.
     * Try processing the transaction again.
     */
    TRANSACTION_DENIED_BY_SERVER(
        "A209",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_SERVER
    ),

    /**
     * Communication timeout.
     * Try processing the transaction again.
     */
    COMMUNICATION_TIMEOUT(
        "A223",
        ProcessingErrorEvent.COMMUNICATION_TIMEOUT
    ),

    /**
     * Invalid menu option.
     * Select a valid menu option.
     */
    INVALID_MENU_OPTION(
        "A239",
        ProcessingErrorEvent.INVALID_MENU_OPTION
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
     * WiFi networks not found.
     * Try scanning for networks again.
     */
    WIFI_NETWORKS_NOT_FOUND(
        "A247",
        ProcessingErrorEvent.WIFI_NETWORKS_NOT_FOUND
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
     * Transaction confirmation in progress.
     * Try again later or send receipt by email/SMS.
     */
    TRANSACTION_CONFIRMATION_IN_PROGRESS(
        "A255",
        ProcessingErrorEvent.TRANSACTION_CONFIRMATION_IN_PROGRESS
    ),

    /**
     * Mobile communication error.
     * Check mobile connection and try again.
     */
    MOBILE_COMMUNICATION_ERROR(
        "A9404",
        ProcessingErrorEvent.MOBILE_COMMUNICATION_ERROR
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
     * Invalid installment value.
     * Installment value must be at least R$5.00.
     */
    INVALID_INSTALLMENT_VALUE(
        "M2006",
        ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT
    ),

    /**
     * Invalid number of installments.
     * Check installment configuration.
     */
    INVALID_INSTALLMENT_COUNT(
        "M2007",
        ProcessingErrorEvent.INVALID_INSTALLMENTS_COUNT
    ),

    /**
     * Invalid installment type.
     * Check installment type configuration.
     */
    INVALID_INSTALLMENT_TYPE(
        "M2010",
        ProcessingErrorEvent.INVALID_INSTALLMENT_METHOD
    ),

    /**
     * Error refunding transaction.
     * Try again later.
     */
    REFUND_ERROR(
        "M2017",
        ProcessingErrorEvent.REFUND_ERROR
    ),

    /**
     * Invalid card number.
     * Check card number and try again.
     */
    INVALID_CARD_NUMBER(
        "M2019",
        ProcessingErrorEvent.INVALID_CARD_NUMBER
    ),

    /**
     * Invalid transaction amount.
     * Amount must be at least R$1.00.
     */
    INVALID_TRANSACTION_AMOUNT(
        "M2020",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    /**
     * Card not identified.
     * Try a different card.
     */
    CARD_NOT_IDENTIFIED(
        "M2022",
        ProcessingErrorEvent.CARD_NOT_IDENTIFIED
    ),

    /**
     * Transaction not authorized by card issuer.
     * Contact card issuer.
     */
    TRANSACTION_NOT_AUTHORIZED_BY_ISSUER(
        "M2035",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_ISSUER
    ),

    /**
     * Invalid card data.
     * Check card data and try again.
     */
    INVALID_CARD_DATA(
        "M2036",
        ProcessingErrorEvent.INVALID_CARD_DATA
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
     * Transaction not authorized.
     * Try a different payment method.
     */
    TRANSACTION_NOT_AUTHORIZED_M2047(
        "M2047",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),

    /**
     * Incorrect password.
     * Verify password and try again.
     */
    INCORRECT_PASSWORD(
        "M2048",
        ProcessingErrorEvent.CHECK_PASSWORD
    ),

    /**
     * Character limit exceeded.
     * Enter maximum 10 characters.
     */
    CHARACTER_LIMIT_EXCEEDED(
        "M2049",
        ProcessingErrorEvent.MAX_CHARACTERS
    ),

    /**
     * Invalid characters.
     * Use only letters and numbers.
     */
    INVALID_CHARACTERS(
        "M2050",
        ProcessingErrorEvent.INVALID_CHARACTERS
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
     * Transaction not found for refund.
     * Transaction may have already been refunded.
     */
    TRANSACTION_NOT_FOUND_FOR_REFUND(
        "M3004",
        ProcessingErrorEvent.TRANSACTION_NOT_FOUND
    ),

    /**
     * Refund time limit exceeded.
     * Refund period has expired.
     */
    REFUND_TIME_LIMIT_EXCEEDED(
        "M3005",
        ProcessingErrorEvent.REFUND_TIME_LIMIT_EXCEEDED
    ),

    /**
     * Transaction cannot be refunded.
     * Contact support for assistance.
     */
    TRANSACTION_CANNOT_BE_REFUNDED(
        "M3006",
        ProcessingErrorEvent.TRANSACTION_CANNOT_BE_REFUNDED
    ),

    /**
     * Transaction cannot be confirmed.
     * Contact support with transaction details.
     */
    TRANSACTION_CANNOT_BE_CONFIRMED(
        "M3009",
        ProcessingErrorEvent.TRANSACTION_CANNOT_BE_CONFIRMED
    ),

    /**
     * Transaction cannot be canceled.
     * Contact support with transaction details.
     */
    TRANSACTION_CANNOT_BE_CANCELED(
        "M3010",
        ProcessingErrorEvent.TRANSACTION_CANNOT_BE_CANCELLED
    ),

    /**
     * Transaction not authorized by PagSeguro.
     * Contact account manager/risk area.
     */
    TRANSACTION_NOT_AUTHORIZED_BY_PAGSEGURO(
        "M3012",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_ACQUIRER
    ),

    /**
     * Seller blocked.
     * Contact account manager.
     */
    SELLER_BLOCKED(
        "M3013",
        ProcessingErrorEvent.SELLER_BLOCKED
    ),

    /**
     * Email not confirmed.
     * Verify email address.
     */
    EMAIL_NOT_CONFIRMED(
        "M3014",
        ProcessingErrorEvent.EMAIL_NOT_CONFIRMED
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
     * International card not supported.
     * Use a domestic card.
     */
    INTERNATIONAL_CARD_NOT_SUPPORTED(
        "M3020",
        ProcessingErrorEvent.INTERNATIONAL_CARD_NOT_SUPPORTED
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
     * Installment configuration error.
     * Contact support with transaction details.
     */
    INSTALLMENT_CONFIGURATION_ERROR(
        "M3022",
        ProcessingErrorEvent.INSTALLMENT_CONFIGURATION_ERROR
    ),

    /**
     * Duplicate reference.
     * Use a unique reference.
     */
    DUPLICATE_REFERENCE(
        "M3023",
        ProcessingErrorEvent.DUPLICATE_REFERENCE
    ),

    /**
     * Transaction not authorized by PagSeguro.
     * Contact account manager/risk area.
     */
    TRANSACTION_NOT_AUTHORIZED_BY_PAGSEGURO_M3024(
        "M3024",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_BY_ACQUIRER
    ),

    /**
     * Invalid reader or activation code.
     * Verify activation code.
     */
    INVALID_READER_OR_ACTIVATION_CODE(
        "M5000",
        ProcessingErrorEvent.INVALID_READER_OR_ACTIVATION_CODE
    ),

    /**
     * Invalid BIN.
     * Try a different card.
     */
    INVALID_BIN(
        "M5001",
        ProcessingErrorEvent.INVALID_BIN
    ),

    /**
     * Invalid holder.
     * Check card holder information.
     */
    INVALID_HOLDER(
        "M5002",
        ProcessingErrorEvent.INVALID_HOLDER
    ),

    /**
     * Invalid reader serial number.
     * Contact support for assistance.
     */
    INVALID_READER_SERIAL_NUMBER(
        "M5003",
        ProcessingErrorEvent.INVALID_READER_SERIAL_NUMBER
    ),

    /**
     * Reader serial number not identified.
     * Contact support for assistance.
     */
    READER_SERIAL_NUMBER_NOT_IDENTIFIED(
        "M5004",
        ProcessingErrorEvent.UNIDENTIFIED_READER_SERIAL_NUMBER
    ),

    /**
     * Inactive reader.
     * Activate reader before use.
     */
    INACTIVE_READER(
        "M5005",
        ProcessingErrorEvent.INACTIVE_READER
    ),

    /**
     * Incorrect activation code.
     * Verify activation code and try again after 30 minutes.
     */
    INCORRECT_ACTIVATION_CODE(
        "M5007",
        ProcessingErrorEvent.INCORRECT_ACTIVATION_CODE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A13(
        "A13",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * SIM card error.
     * Contact support for assistance.
     */
    SIM_CARD_ERROR_A25(
        "A25",
        ProcessingErrorEvent.SIM_CARD_ERROR
    ),

    /**
     * WiFi authentication error.
     * Check WiFi credentials and try again.
     */
    WIFI_AUTHENTICATION_ERROR_A32(
        "A32",
        ProcessingErrorEvent.WIFI_AUTH_ERROR
    ),

    /**
     * Connection error.
     * Check SIM card and WiFi connection.
     */
    CONNECTION_ERROR_A34(
        "A34",
        ProcessingErrorEvent.NETWORK_ATTACHMENT_ERROR
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A35(
        "A35",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A37(
        "A37",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A38(
        "A38",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A39(
        "A39",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Communication time exceeded.
     * Try processing the transaction again.
     */
    COMMUNICATION_TIMEOUT_A39_ALT(
        "A39",
        ProcessingErrorEvent.COMMUNICATION_TIMEOUT
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A40(
        "A40",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A41(
        "A41",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A42(
        "A42",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A43(
        "A43",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Communication failure.
     * Try processing the transaction again.
     */
    COMMUNICATION_FAILURE_A43_ALT(
        "A43",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A44(
        "A44",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Communication failure.
     * Try processing the transaction again.
     */
    COMMUNICATION_FAILURE_A44_ALT(
        "A44",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * Communication failure.
     * Try processing the transaction again.
     */
    COMMUNICATION_FAILURE_A45_ALT(
        "A45",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A46(
        "A46",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Communication failure.
     * Try processing the transaction again.
     */
    COMMUNICATION_FAILURE_A46_ALT(
        "A46",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * Transaction failure.
     * Try processing the transaction again.
     */
    TRANSACTION_FAILURE_A47(
        "A47",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    /**
     * Communication failure.
     * Try processing the transaction again.
     */
    COMMUNICATION_FAILURE_A47_ALT(
        "A47",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * SSL error.
     * Try processing the transaction again.
     */
    SSL_ERROR_A48(
        "A48",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * SSL error.
     * Try processing the transaction again.
     */
    SSL_ERROR_A49(
        "A49",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    /**
     * WiFi communication error.
     * Try processing the transaction again.
     */
    WIFI_COMMUNICATION_ERROR_A50(
        "A50",
        ProcessingErrorEvent.WIFI_NETWORK_UNAVAILABLE
    ),

    /**
     * Communication timeout.
     * Try processing the transaction again.
     */
    COMMUNICATION_TIMEOUT_A58(
        "A58",
        ProcessingErrorEvent.COMMUNICATION_TIMEOUT
    ),

    /**
     * Network attachment error.
     * Wait and try again.
     */
    NETWORK_ATTACHMENT_ERROR_A59(
        "A59",
        ProcessingErrorEvent.NETWORK_ATTACHMENT_ERROR
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
     * Communication problem.
     * Contact support with transaction details.
     */
    COMMUNICATION_PROBLEM_A104(
        "A104",
        ProcessingErrorEvent.COMMUNICATION_ERROR
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
     * Refund error.
     * Try again later.
     */
    REFUND_ERROR_A207(
        "A207",
        ProcessingErrorEvent.REFUND_ERROR
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
     * WiFi authentication error.
     * Check WiFi credentials and try again.
     */
    WIFI_AUTHENTICATION_ERROR_A246(
        "A246",
        ProcessingErrorEvent.WIFI_AUTH_ERROR
    ),

    /**
     * Invalid installment value for payment.
     * Check installment value configuration.
     */
    INVALID_INSTALLMENT_VALUE_M2018(
        "M2018",
        ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT
    ),

    /**
     * Transaction cannot be refunded.
     * Contact support for assistance.
     */
    TRANSACTION_CANNOT_BE_REFUNDED_M3008(
        "M3008",
        ProcessingErrorEvent.TRANSACTION_CANNOT_BE_REFUNDED
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
     * Transaction failed.
     * Check returned message.
     */
    TRANSACTION_FAILED(
        "-1004",
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),
    
    /**
     * Invalid transaction response buffer.
     * Check last transaction status.
     */
    INVALID_TRANSACTION_RESPONSE(
        "-1005",
        ProcessingErrorEvent.INVALID_TRANSACTION_BUFFER
    ),
    
    /**
     * Transaction value parameter cannot be null.
     * Check implementation.
     */
    NULL_TRANSACTION_VALUE(
        "-1006",
        ProcessingErrorEvent.NULL_TRANSACTION_RESULT
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
     * Sale code parameter cannot be null.
     * Check implementation.
     */
    NULL_SALE_CODE(
        "-1008",
        ProcessingErrorEvent.TRANSACTION_NULL_SALE_CODE
    ),
    
    /**
     * Transaction result parameter cannot be null.
     * Check implementation.
     */
    NULL_TRANSACTION_RESULT(
        "-1009",
        ProcessingErrorEvent.NULL_TRANSACTION_RESULT
    ),
    
    /**
     * Connection driver not found.
     * Check file directory.
     */
    CONNECTION_DRIVER_NOT_FOUND(
        "-1010",
        ProcessingErrorEvent.CONNECTION_DRIVER_NOT_FOUND
    ),
    
    /**
     * Connection driver error.
     * Reinstall connection driver files.
     */
    CONNECTION_DRIVER_ERROR(
        "-1011",
        ProcessingErrorEvent.CONNECTION_DRIVER_ERROR
    ),
    
    /**
     * Invalid sale value format.
     * Value must be integer without decimal point.
     */
    INVALID_SALE_VALUE_FORMAT(
        "-1012",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT_FORMAT
    ),
    
    /**
     * Sale code exceeds length limit.
     * Truncate sale code to maximum 10 digits.
     */
    SALE_CODE_TOO_LONG(
        "-1013",
        ProcessingErrorEvent.TRANSACTION_CODE_TOO_LONG
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
     * No last transaction data.
     * Retry transaction.
     */
    NO_LAST_TRANSACTION_DATA(
        "-1018",
        ProcessingErrorEvent.NO_LAST_TRANSACTION_DATA
    ),
    
    /**
     * Terminal communication error.
     * Check last transaction status.
     */
    TERMINAL_COMMUNICATION_ERROR(
        "-1019",
        ProcessingErrorEvent.TERMINAL_COMMUNICATION_ERROR
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
     * Invalid amount.
     * Check payment amount and try again. Minimum: R$ 1.00.
     */
    INVALID_VALUE_1031(
        "-1031",
        ProcessingErrorEvent.TRANSACTION_INVALID_AMOUNT
    ),
    
    /**
     * Invalid installment.
     * Check installment count, try again.
     */
    INVALID_INSTALLMENT_1032(
        "-1032",
        ProcessingErrorEvent.INVALID_INSTALLMENTS_COUNT
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
     * Missing installment coefficients.
     * Invalid buyer installment coefficients. Try new login/activation.
     */
    MISSING_INSTALLMENT_COEFFICIENTS(
        "-1034",
        ProcessingErrorEvent.MISSING_INSTALLMENT_COEFFICIENTS
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
     * Card reader not initialized.
     * Check if initializeAndActivatePinpad was called correctly.
     */
    CARD_READER_NOT_INITIALIZED(
        "-1036",
        ProcessingErrorEvent.CARD_READER_NOT_INITIALIZED
    ),
    
    /**
     * Invalid card reader.
     * Check card reader name used in code.
     */
    INVALID_CARD_READER(
        "-1037",
        ProcessingErrorEvent.INVALID_CARD
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
     * Font library not installed.
     * Restart device and try again.
     */
    FONT_LIBRARY_NOT_INSTALLED(
        "-5009",
        ProcessingErrorEvent.FONT_LIBRARY_NOT_INSTALLED
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
