package br.com.ticpass.pos.core.queue.error

import br.com.ticpass.pos.core.queue.core.BaseProcessingEvent

/**
 * Processing error events emitted during item processing
 */
sealed class ProcessingErrorEvent : BaseProcessingEvent {
    /**
     * Product quantity overflow.
     */
    object PRODUCT_QUANTITY_OVERFLOW : ProcessingErrorEvent()

    /**
     * NFC tag not found.
     */
    object NFC_TAG_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Error reading NFC tag customer data.
     */
    object NFC_READING_TAG_CUSTOMER_DATA_ERROR : ProcessingErrorEvent()

    /**
     * Error processing NFC tag customer data.
     */
    object NFC_PROCESSING_TAG_CUSTOMER_DATA_ERROR : ProcessingErrorEvent()

    /**
     * NFC tag customer PIN incorrect.
     */
    object NFC_TAG_CUSTOMER_PIN_INCORRECT : ProcessingErrorEvent()

    /**
     * Merchant invalid PIX key.
     */
    object INVALID_PIX_KEY : ProcessingErrorEvent()

    /**
     * NFC tag invalid keys.
     */
    object NFC_TAG_INVALID_KEYS : ProcessingErrorEvent()

    /**
     * NFC missing keys.
     */
    object NFC_TAG_MISSING_KEYS : ProcessingErrorEvent()

    /**
     * NFC tag invalid key length.
     * Check if the key length is correct.
     */
    object NFC_TAG_INVALID_KEY_LENGTH : ProcessingErrorEvent()

    /**
     * NFC write error.
     * Check if the tag is writable and try again.
     */
    object NFC_WRITE_ERROR : ProcessingErrorEvent()

    /**
     * NFC read error.
     */
    object NFC_READ_ERROR : ProcessingErrorEvent()

    /**
     * NFC tag invalid key format.
     */
    object NFC_TAG_INVALID_KEY_FORMAT : ProcessingErrorEvent()

    /**
     * Processor not found.
     * No suitable processor found.
     */
    object PROCESSOR_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Error refunding transaction.
     * Try again later.
     */
    object REFUND_ERROR : ProcessingErrorEvent()

    /**
     * Generic retry error.
     * Try again.
     */
    object GENERIC_RETRY : ProcessingErrorEvent()

    /**
     * Invalid transaction response buffer.
     * Check last transaction status.
     */
    object INVALID_TRANSACTION_BUFFER : ProcessingErrorEvent()

    /**
     * Invalid file.
     * Try again with valid file.
     */
    object INVALID_FILE : ProcessingErrorEvent()

    /**
     * Unexpected error. Try again.
     */
    object UNEXPECTED_ERROR : ProcessingErrorEvent()

    /**
     * Attempts exceeded. Use another card.
     */
    object ATTEMPTS_EXCEEDED : ProcessingErrorEvent()

    /**
     * Installment not allowed for this transaction type. Use another card or don't use installments.
     */
    object INSTALLMENT_NOT_ALLOWED : ProcessingErrorEvent()

    /**
     * Installment not allowed for prepaid card.
     */
    object INSTALLMENT_NOT_ALLOWED_PREPAID : ProcessingErrorEvent()

    /**
     * Invalid installment method.
     * Check if the installment method is valid for the transaction.
     */
    object INVALID_INSTALLMENT_METHOD : ProcessingErrorEvent()

    /**
     * Operation not performed.
     * The operation was not performed successfully.
     */
    object OPERATION_NOT_PERFORMED : ProcessingErrorEvent()

    /**
     * Use credit function to perform pre-authorization.
     */
    object USE_CREDIT_FOR_PREAUTH : ProcessingErrorEvent()

    /**
     * Capture amount is greater than pre-authorized amount.
     */
    object CAPTURE_AMOUNT_EXCEEDED : ProcessingErrorEvent()

    /**
     * Card brand not allowed for pre-authorization.
     */
    object BRAND_NOT_ALLOWED_PREAUTH : ProcessingErrorEvent()

    /**
     * Pre-authorization cannot be captured. Validity expired.
     */
    object PREAUTH_EXPIRED : ProcessingErrorEvent()

    /**
     * Pre-authorization not enabled. Contact support.
     */
    object PREAUTH_NOT_ENABLED : ProcessingErrorEvent()

    /**
     * Card not identified. Use another card.
     */
    object CARD_NOT_IDENTIFIED : ProcessingErrorEvent()

    /**
     * Operation timeout exceeded. Try again.
     */
    object OPERATION_TIMEOUT : ProcessingErrorEvent()

    /**
     * Invalid card data.
     */
    object INVALID_CARD_DATA : ProcessingErrorEvent()

    /**
     * Minimum installment amount is $5.00.
     */
    object MINIMUM_INSTALLMENT_AMOUNT : ProcessingErrorEvent()

    /**
     * Amount lower than allowed.
     */
    object AMOUNT_TOO_LOW : ProcessingErrorEvent()

    /**
     * Invalid transaction amount.
     */
    object INVALID_TRANSACTION_AMOUNT : ProcessingErrorEvent()

    /**
     * Pre-authorization quantity exceeded.
     */
    object PREAUTH_QUANTITY_EXCEEDED : ProcessingErrorEvent()

    /**
     * Operation not allowed. Request in progress.
     */
    object REQUEST_IN_PROGRESS : ProcessingErrorEvent()

    /**
     * Request cannot be completed.
     * Contact support with transaction details.
     */
    object REQUEST_CANNOT_BE_COMPLETED : ProcessingErrorEvent()

    /**
     * Request cannot be executed.
     * Contact support with transaction details.
     */
    object REQUEST_CANNOT_BE_EXECUTED : ProcessingErrorEvent()

    /**
     * Incorrect code.
     * Check if the activation code entered is correct. Wait 30 minutes to try again.
     */
    object INCORRECT_ACTIVATION_CODE : ProcessingErrorEvent()

    /**
     * Operation time exceeded.
     * Seller should process the sale again.
     */
    object OPERATION_TIME_EXCEEDED : ProcessingErrorEvent()

    /**
     * Invalid POS key.
     * Contact acquirer technical support.
     */
    object INVALID_POS_KEY : ProcessingErrorEvent()

    /**
     * To continue selling update your device.
     */
    object UPDATE_DEVICE : ProcessingErrorEvent()

    /**
     * Transaction cannot be reversed.
     */
    object TRANSACTION_CANNOT_BE_REVERSED : ProcessingErrorEvent()

    /**
     * Transaction cannot be confirmed.
     * Contact support with transaction details.
     */
    object TRANSACTION_CANNOT_BE_CONFIRMED : ProcessingErrorEvent()

    /**
     * Transaction already reversed.
     * No action needed reversal already processed.
     */
    object TRANSACTION_ALREADY_REVERSED : ProcessingErrorEvent()

    /**
     * Transaction not authorized.
     */
    object TRANSACTION_NOT_AUTHORIZED : ProcessingErrorEvent()

    /**
     * Transaction not authorized by acquirer.
     * Contact your commercial manager/risk area.
     */
    object TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER : ProcessingErrorEvent()

    /**
     * Communication problem try again.
     * Try again.
     */
    object COMMUNICATION_ERROR : ProcessingErrorEvent()

    /**
     * Operation cancelled.
     * Try again.
     */
    object OPERATION_CANCELLED : ProcessingErrorEvent()

    /**
     * Card with error or poorly inserted.
     * Try again if it doesn't work use another card.
     */
    object CARD_ERROR_POORLY_INSERTED : ProcessingErrorEvent()

    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    object INVALID_CARD_NOT_ACCEPTED : ProcessingErrorEvent()

    /**
     * Invalid card.
     * Card is not accepted. Use another card.
     */
    object INVALID_CARD_USE_ANOTHER : ProcessingErrorEvent()

    /**
     * Not authorized by acquirer.
     * Try again if it persists contact your commercial manager.
     */
    object TRANSACTION_NOT_AUTHORIZED_BY_ACQUIRER : ProcessingErrorEvent()

    /**
     * Transaction not authorized by issuer.
     * Try again if it persists contact your commercial manager.
     */
    object TRANSACTION_NOT_AUTHORIZED_BY_ISSUER : ProcessingErrorEvent()

    /**
     * Invalid card reader.
     * Check card reader name used in code.
     */
    object INVALID_CARD : ProcessingErrorEvent()

    /**
     * Print file not found.
     * Try again with valid file.
     */
    object PRINT_FILE_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Invalid bin.
     * Use same card as original transaction.
     */
    object INVALID_BIN : ProcessingErrorEvent()

    /**
     * Invalid holder.
     * Use same card as original transaction.
     */
    object INVALID_HOLDER : ProcessingErrorEvent()

    /**
     * Invalid card number.
     * Use same card as original transaction.
     */
    object INVALID_CARD_NUMBER : ProcessingErrorEvent()

    /**
     * Reader serial number is invalid.
     */
    object INVALID_READER_SERIAL_NUMBER : ProcessingErrorEvent()

    /**
     * Reader serial number not identified.
     * Contact support for assistance.
     */
    object UNIDENTIFIED_READER_SERIAL_NUMBER : ProcessingErrorEvent()

    /**
     * Inactive reader.
     * Activate reader before use.
     */
    object INACTIVE_READER : ProcessingErrorEvent()

    /**
     * Connection refused.
     * Try processing the transaction again.
     */
    object CONNECTION_REFUSED : ProcessingErrorEvent()

    /**
     * Transaction not found for refund.
     * Transaction may have already been refunded.
     */
    object TRANSACTION_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Invalid input mode.
     * If the transaction was processed with chip authentication
     * the reversal must be done with chip if it was processed with stripe reversal with stripe.
     */
    object INVALID_INPUT_MODE : ProcessingErrorEvent()

    /**
     * Reader without required key.
     * Force new table loading.
     */
    object READER_WITHOUT_KEY : ProcessingErrorEvent()

    /**
     * Printer busy.
     * Wait for printer to finish current task.
     */
    object PRINTER_BUSY : ProcessingErrorEvent()

    /**
     * WiFi networks not found.
     * Try scanning for networks again.
     */
    object WIFI_NETWORKS_NOT_FOUND : ProcessingErrorEvent()

    /**
     * WiFi authentication error.
     * Check WiFi credentials and try again.
     */
    object WIFI_AUTH_ERROR : ProcessingErrorEvent()

    /**
     * File operation failure.
     * Try again or restart terminal.
     */
    object FILE_OPERATION_FAILURE : ProcessingErrorEvent()

    /**
     * User canceled operation.
     * User pressed cancel button.
     */
    object CANCELLED_BY_USER : ProcessingErrorEvent()

    /**
     * Invalid menu option.
     * Select a valid menu option.
     */
    object INVALID_MENU_OPTION : ProcessingErrorEvent()

    /**
     * Server operation failure.
     * Try again.
     */
    object ACQUIRER_SERVER_ERROR : ProcessingErrorEvent()

    /**
     * Communication timeout.
     * Try processing the transaction again.
     */
    object COMMUNICATION_TIMEOUT : ProcessingErrorEvent()

    /**
     * Contactless not authorized insert card.
     * Try again if it persists contact your commercial manager.
     */
    object CONTACTLESS_NOT_AUTHORIZED : ProcessingErrorEvent()

    /**
     * Invalid selected option.
     * Contactless product different from the one selected for payment.
     */
    object INVALID_SELECTED_OPTION : ProcessingErrorEvent()

    /**
     * Card reach not allowed.
     * Use chip instead of contactless.
     */
    object CARD_REACH_NOT_ALLOWED : ProcessingErrorEvent()

    /**
     * NFC tag reach timed out.
     */
    object NFC_TAG_REACH_TIMEOUT : ProcessingErrorEvent()

    /**
     * NFC unsupported card model.
     */
    object NFC_UNSUPPORTED_TAG_MODEL : ProcessingErrorEvent()

    /**
     * Card not accepted.
     * Brand not accepted. Use a card with one of the accepted brands.
     */
    object CARD_BRAND_NOT_ACCEPTED : ProcessingErrorEvent()

    /**
     * Card invalidated.
     * Card is blocked. Contact the issuing bank or use another card.
     */
    object CARD_INVALIDATED : ProcessingErrorEvent()

    /**
     * Blocked card.
     * Card is blocked. Unblock or use another card.
     */
    object BLOCKED_CARD : ProcessingErrorEvent()

    /**
     * Expired card.
     * Use another card.
     */
    object EXPIRED_CARD : ProcessingErrorEvent()

    /**
     * Internal pinpad error.
     * Equipment should be replaced.
     */
    object INTERNAL_PINPAD_ERROR : ProcessingErrorEvent()

    /**
     * Maximum time limit for operation exceeded.
     * Card was not inserted.
     */
    object MAX_TIME_EXCEEDED : ProcessingErrorEvent()

    /**
     * Pinpad error.
     * Equipment should be replaced.
     */
    object PINPAD_ERROR : ProcessingErrorEvent()

    /**
     * Use chip for this transaction / Invalid payment method.
     * Pass the transaction with chip / Check if the card has the chosen payment method enabled.
     */
    object USE_CHIP_INVALID_PAYMENT : ProcessingErrorEvent()

    /**
     * Response time exceeded.
     * Try again.
     */
    object RESPONSE_TIME_EXCEEDED : ProcessingErrorEvent()

    /**
     * Could not locate the reference is duplicated.
     */
    object COULD_NOT_LOCATE_REFERENCE_DUPLICATED : ProcessingErrorEvent()

    /**
     * Read error try again.
     * Try again.
     */
    object READ_ERROR : ProcessingErrorEvent()

    /**
     * Printer malfunction.
     * Try again or check printer hardware.
     */
    object PRINTER_MALFUNCTION : ProcessingErrorEvent()

    /**
     * Overheating problem.
     * Printer reporting overheating.
     */
    object PRINTER_OVERHEATING : ProcessingErrorEvent()

    /**
     * Invalid print data format.
     * Try a different file format.
     */
    object PRINT_DATA_FORMAT_ERROR : ProcessingErrorEvent()

    /**
     * No GSM signal.
     * Move to an area with better reception.
     */
    object NO_GSM_SIGNAL : ProcessingErrorEvent()

    /**
     * Socket connection error.
     * Try processing the transaction again.
     */
    object SOCKET_CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * Printer error.
     * Check printer status and try again.
     */
    object PRINTER_ERROR : ProcessingErrorEvent()

    /**
     * Generic error with error code and message.
     */
    object GENERIC : ProcessingErrorEvent()

    /**
     * Message buffer overflow.
     * Contact support with logs.
     */
    object MESSAGE_BUFFER_OVERFLOW : ProcessingErrorEvent()

    /**
     * Transaction result parameter cannot be null.
     * Check implementation.
     */
    object NULL_TRANSACTION_RESULT : ProcessingErrorEvent()

    /**
     * Token not found.
     * Reauthenticate.
     */
    object TOKEN_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Root permission detected.
     * Remove root permission from device.
     */
    object ROOT_PERMISSION_DETECTED : ProcessingErrorEvent()

    /**
     * No authentication data.
     * Perform authentication.
     */
    object NO_AUTHENTICATION_DATA : ProcessingErrorEvent()

    /**
     * Low voltage.
     * Check device battery below 15% may cause this error.
     */
    object LOW_BATTERY : ProcessingErrorEvent()

    /**
     * Data packet format error.
     * Restart device and try again.
     */
    object DATA_PACKET_FORMAT : ProcessingErrorEvent()

    /**
     * Image processing failed.
     * Try again with different image.
     */
    object IMAGE_PROCESSING_FAILED : ProcessingErrorEvent()

    /**
     * SDK not available for printing.
     * Restart device and try again.
     */
    object SDK_PRINT_UNAVAILABLE : ProcessingErrorEvent()

    /**
     * Data package too long.
     * Check file size.
     */
    object DATA_PACKAGE_TOO_LONG : ProcessingErrorEvent()

    /**
     * Font library not installed.
     * Restart device and try again.
     */
    object FONT_LIBRARY_NOT_INSTALLED : ProcessingErrorEvent()

    /**
     * Printing unfinished.
     * Check printer paper and try again.
     */
    object PRINTING_UNFINISHED : ProcessingErrorEvent()

    /**
     * Invalid transaction amount.
     * Amount must be greater than zero.
     */
    object TRANSACTION_INVALID_AMOUNT : ProcessingErrorEvent()

    /**
     * Card reader not initialized.
     * Check if initializeAndActivatePinpad was called correctly.
     */
    object CARD_READER_NOT_INITIALIZED : ProcessingErrorEvent()

    /**
     * Invalid device identification.
     * Check device registration.
     */
    object INVALID_DEVICE_ID : ProcessingErrorEvent()

    /**
     * Missing installment coefficients.
     * Invalid buyer installment coefficients. Try new login/activation.
     */
    object MISSING_INSTALLMENT_COEFFICIENTS : ProcessingErrorEvent()

    /**
     * Authentication error.
     * Check username/password or activation code and try again.
     */
    object AUTHENTICATION_ERROR : ProcessingErrorEvent()

    /**
     * No last transaction data.
     * Retry transaction.
     */
    object NO_LAST_TRANSACTION_DATA : ProcessingErrorEvent()

    /**
     * Terminal communication error.
     * Check last transaction status.
     */
    object TERMINAL_COMMUNICATION_ERROR : ProcessingErrorEvent()

    /**
     * Application name required.
     * Set name and version with setVersionName(String String).
     */
    object APPLICATION_NAME_REQUIRED : ProcessingErrorEvent()

    /**
     * Application name too long.
     * Limit application name to 25 characters.
     */
    object APPLICATION_NAME_TOO_LONG : ProcessingErrorEvent()

    /**
     * Application version too long.
     * Limit application version to 10 characters.
     */
    object APPLICATION_VERSION_TOO_LONG : ProcessingErrorEvent()

    /**
     * Corrupted reception buffer.
     * Retry transaction.
     */
    object CORRUPTED_RECEPTION_BUFFER : ProcessingErrorEvent()

    /**
     * Sale code exceeds length limit.
     * Truncate sale code to maximum 10 digits.
     */
    object TRANSACTION_CODE_TOO_LONG : ProcessingErrorEvent()

    /**
     * Invalid sale value format.
     * Value must be integer without decimal point.
     */
    object INVALID_TRANSACTION_AMOUNT_FORMAT : ProcessingErrorEvent()

    /**
     * Total transaction value parameter cannot be null.
     * Check implementation.
     */
    object TRANSACTION_NULL_AMOUNT : ProcessingErrorEvent()

    /**
     * Connection driver error.
     * Reinstall connection driver files.
     */
    object CONNECTION_DRIVER_ERROR : ProcessingErrorEvent()

    /**
     * Connection driver not found.
     * Check file directory.
     */
    object CONNECTION_DRIVER_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Sale code parameter cannot be null.
     * Check implementation.
     */
    object TRANSACTION_NULL_SALE_CODE : ProcessingErrorEvent()

    /**
     * Invalid application parameter.
     * Contact support with logs.
     */
    object INVALID_APPLICATION_PARAMETER : ProcessingErrorEvent()

    /**
     * Duplicate reference.
     * Use a unique reference.
     */
    object DUPLICATE_REFERENCE : ProcessingErrorEvent()

    /**
     * International card not supported.
     * Use a domestic card.
     */
    object INTERNATIONAL_CARD_NOT_SUPPORTED : ProcessingErrorEvent()

    /**
     * Installment configuration error.
     * Contact support with transaction details.
     */
    object INSTALLMENT_CONFIGURATION_ERROR : ProcessingErrorEvent()

    /**
     * Authentication required.
     * User must authenticate to continue.
     */
    object AUTHENTICATION_REQUIRED : ProcessingErrorEvent()

    /**
     * Transaction cannot be refunded.
     * Transaction is not eligible for refund.
     */
    object TRANSACTION_CANNOT_BE_REFUNDED : ProcessingErrorEvent()

    /**
     * Refund time limit exceeded.
     * Refund period has expired.
     */
    object REFUND_TIME_LIMIT_EXCEEDED : ProcessingErrorEvent()

    /**
     * Terminal update required.
     * Update terminal to continue selling.
     */
    object TERMINAL_UPDATE_REQUIRED : ProcessingErrorEvent()

    /**
     * Operation not completed.
     * Try again later.
     */
    object OPERATION_NOT_COMPLETED : ProcessingErrorEvent()

    /**
     * App update required.
     * Update application to continue.
     */
    object APP_UPDATE : ProcessingErrorEvent()

    /**
     * Invalid characters in input.
     * Check input for invalid characters and try again.
     */
    object INVALID_CHARACTERS : ProcessingErrorEvent()

    /**
     * Transaction denied by server.
     * Try processing the transaction again.
     */
    object TRANSACTION_NOT_AUTHORIZED_BY_SERVER : ProcessingErrorEvent()

    /**
     * Terminal not ready.
     * Try again.
     */
    object TERMINAL_NOT_CONFIGURED : ProcessingErrorEvent()

    /**
     * Internet connection error.
     * Check internet connection and try again.
     */
    object INTERNET_CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * Connection error with server.
     * Try processing the transaction again.
     */
    object ACQUIRER_SERVER_CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * Modem initialization pending.
     * Wait for modem to initialize.
     */
    object MODEM_INITIALIZATION_PENDING : ProcessingErrorEvent()

    /**
     * WiFi not connected.
     * Configure WiFi connection.
     */
    object WIFI_NOT_CONNECTED : ProcessingErrorEvent()

    /**
     * Network attachment error.
     * Contact support for assistance.
     */
    object NETWORK_ATTACHMENT_ERROR : ProcessingErrorEvent()

    /**
     * GPRS connection error.
     * Check network settings and try again.
     */
    object GPRS_CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * Host response timeout.
     * Try processing the transaction again.
     */
    object HOST_RESPONSE_TIMEOUT : ProcessingErrorEvent()

    /**
     * Try again later.
     * Contact support if the problem persists.
     */
    object TRY_AGAIN_LATER : ProcessingErrorEvent()

    /**
     * Telecommunication provider unavailable.
     * Try processing the transaction again.
     */
    object TELECOM_PROVIDER_UNAVAILABLE : ProcessingErrorEvent()

    /**
     * Operation rejected by card.
     * Try with a different card.
     */
    object OPERATION_REJECTED_BY_CARD : ProcessingErrorEvent()

    /**
     * Operation not authorized by host.
     * Contact support with transaction details.
     */
    object OPERATION_NOT_AUTHORIZED_BY_HOST : ProcessingErrorEvent()

    /**
     * Device not activated.
     * Activate the payment terminal.
     */
    object DEVICE_NOT_ACTIVATED : ProcessingErrorEvent()

    /**
     * Cash only transaction.
     * Transaction can only be processed in cash.
     */
    object CASH_ONLY_TRANSACTION : ProcessingErrorEvent()

    /**
     * Invalid transaction.
     * Transaction is not valid. Check transaction details.
     */
    object INVALID_TRANSACTION : ProcessingErrorEvent()

    /**
     * Remove card before proceeding.
     * Remove the card from the reader before continuing.
     */
    object REMOVE_CARD_BEFORE_PROCEEDING : ProcessingErrorEvent()

    /**
     * Use CHIP for this transaction.
     * Process using chip authentication method.
     */
    object CHIP_REQUIRED : ProcessingErrorEvent()

    /**
     * Error confirming transaction.
     * Try processing the transaction again.
     */
    object TRANSACTION_CONFIRMATION_ERROR : ProcessingErrorEvent()

    /**
     * Only Total Refund Allowed.
     * Perform full refund instead of partial.
     */
    object ONLY_TOTAL_REFUND_ALLOWED : ProcessingErrorEvent()

    /**
     * Operation not performed. Enter password for this operation.
     * Password required to complete operation.
     */
    object PASSWORD_REQUIRED : ProcessingErrorEvent()

    /**
     * TLV response with larger than expected size.
     * Contact support with transaction details.
     */
    object TLV_RESPONSE_TOO_LARGE : ProcessingErrorEvent()

    /**
     * Transaction already refunded.
     * No action needed refund already processed.
     */
    object TRANSACTION_ALREADY_REFUNDED : ProcessingErrorEvent()

    /**
     * Invalid entry mode.
     * Use same entry method as original transaction.
     */
    object INVALID_ENTRY_MODE : ProcessingErrorEvent()

    /**
     * Connection error.
     * Check SIM card and WiFi network.
     */
    object CONNECTION_ERROR_SIM_WIFI : ProcessingErrorEvent()

    /**
     * WiFi connection error.
     * Check your connection.
     */
    object WIFI_CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * Connection error.
     * Check internet connection.
     */
    object CONNECTION_ERROR_NO_INTERNET : ProcessingErrorEvent()

    /**
     * Connection error.
     * Check connection and try again.
     */
    object CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * Overvoltage problem.
     * Printer reporting overvoltage.
     */
    object PRINTER_OVERVOLTAGE : ProcessingErrorEvent()

    /**
     * Not connected to WiFi network.
     * Configure WiFi.
     */
    object NOT_CONNECTED_TO_WIFI : ProcessingErrorEvent()

    /**
     * WiFi network unavailable.
     * Check WiFi connection and try again.
     */
    object WIFI_NETWORK_UNAVAILABLE : ProcessingErrorEvent()

    /**
     * Operation not authorized.
     * Contact support for assistance.
     */
    object OPERATION_NOT_AUTHORIZED : ProcessingErrorEvent()

    /**
     * Printer out of paper.
     * Replace paper roll and try again.
     */
    object PRINTER_OUT_OF_PAPER : ProcessingErrorEvent()

    /**
     * PPP authentication failure.
     * Contact support center.
     */
    object PPP_AUTH_FAILURE : ProcessingErrorEvent()

    /**
     * No network signal.
     * Try again.
     */
    object NO_NETWORK_SIGNAL : ProcessingErrorEvent()

    /**
     * SIM card missing.
     * Check SIM card and try again.
     */
    object SIM_CARD_MISSING : ProcessingErrorEvent()

    /**
     * Network operator timeout.
     * Check network connection and try again.
     */
    object NETWORK_OPERATOR_TIMEOUT : ProcessingErrorEvent()

    /**
     * SIM card error.
     * Check SIM card and try again.
     */
    object SIM_CARD_ERROR : ProcessingErrorEvent()

    /**
     * SIM card not responding.
     * Wait for SIM card to respond.
     */
    object SIM_CARD_NOT_RESPONDING : ProcessingErrorEvent()

    /**
     * Network operator unavailable.
     * Check network connection and try again.
     */
    object NETWORK_OPERATOR_UNAVAILABLE : ProcessingErrorEvent()

    /**
     * Card operation failed.
     * Unexpected error by terminal.
     */
    object CARD_OPERATION_FAILED : ProcessingErrorEvent()

    /**
     * Device deactivated.
     * Device is not activated.
     */
    object DEVICE_DEACTIVATED : ProcessingErrorEvent()

    /**
     * No message to display.
     * No last error message to display.
     */
    object NO_MESSAGE : ProcessingErrorEvent()

    /**
     * Invalid parameter.
     * Check the parameter and try again.
     */
    object INVALID_PARAMETER : ProcessingErrorEvent()

    /**
     * Initialization error.
     * Restart terminal and try again.
     */
    object INITIALIZATION_ERROR : ProcessingErrorEvent()

    /**
     * Mobile communication error.
     * Check mobile connection and try again.
     */
    object MOBILE_COMMUNICATION_ERROR : ProcessingErrorEvent()

    /**
     * Transaction failed.
     * Check returned message.
     */
    object TRANSACTION_FAILURE : ProcessingErrorEvent()

    /**
     * Transaction confirmation in progress.
     * Try again later or send receipt by email/SMS.
     */
    object TRANSACTION_CONFIRMATION_IN_PROGRESS : ProcessingErrorEvent()

    /**
     * Table loading error.
     * Reinitialize (reload tables).
     */
    object TABLE_LOADING_ERROR : ProcessingErrorEvent()

    /**
     * Print error due to low battery.
     * Connect charger and try again.
     */
    object PRINT_ERROR_LOW_BATTERY : ProcessingErrorEvent()

    /**
     * Invalid reader or activation code.
     * Verify activation code.
     */
    object INVALID_READER_OR_ACTIVATION_CODE : ProcessingErrorEvent()

    /**
     * Could not configure installment.
     */
    object COULD_NOT_CONFIGURE_INSTALLMENT : ProcessingErrorEvent()

    /**
     * Please log in to the app again.
     * Redo the terminal activation.
     */
    object PLEASE_LOGIN_AGAIN : ProcessingErrorEvent()

    /**
     * Cannot make sale with international card.
     */
    object INTERNATIONAL_CARD_NOT_ALLOWED : ProcessingErrorEvent()

    /**
     * Try again.
     */
    object TRY_AGAIN : ProcessingErrorEvent()

    /**
     * Email not confirmed.
     * Verify email address.
     */
    object EMAIL_NOT_CONFIRMED : ProcessingErrorEvent()

    /**
     * Seller blocked.
     * Contact account manager.
     */
    object SELLER_BLOCKED : ProcessingErrorEvent()

    /**
     * Maximum date for transaction reversal has been exceeded.
     */
    object MAX_DATE_FOR_REVERSAL_EXCEEDED : ProcessingErrorEvent()

    /**
     * Update the app version to proceed with the operation.
     */
    object UPDATE_APP : ProcessingErrorEvent()

    /**
     * Please enter only letters and numbers.
     * The userReference field only accepts letters and numbers.
     */
    object ONLY_LETTERS_AND_NUMBERS : ProcessingErrorEvent()

    /**
     * Please check your password.
     */
    object CHECK_PASSWORD : ProcessingErrorEvent()

    /**
     * Character limit exceeded.
     * Enter maximum 10 characters.
     */
    object MAX_CHARACTERS : ProcessingErrorEvent()

    /**
     * Account closed.
     * Contact Acquirer.
     */
    object ACCOUNT_CLOSED : ProcessingErrorEvent()

    /**
     * Could not identify the card.
     */
    object COULD_NOT_IDENTIFY_CARD : ProcessingErrorEvent()

    /**
     * Error reversing transaction.
     * Please try again later.
     */
    object ERROR_REVERSING_TRANSACTION : ProcessingErrorEvent()

    /**
     * Invalid amount for installment.
     */
    object INVALID_INSTALLMENT_AMOUNT : ProcessingErrorEvent()

    /**
     * Invalid transaction.
     * Perform the sale in cash.
     */
    object INVALID_TRANSACTION_PERFORM_CASH : ProcessingErrorEvent()

    /**
     * Invalid number of installments.
     */
    object INVALID_INSTALLMENTS_COUNT : ProcessingErrorEvent()

    /**
     * Feature unavailable at the moment.
     * Process through online banking service.
     */
    object FEATURE_UNAVAILABLE : ProcessingErrorEvent()

    /**
     * Error validating Response Message.
     * Contact technical support.
     */
    object ERROR_RESPONSE_MESSAGE_VALIDATION : ProcessingErrorEvent()

    /**
     * Only total reversal allowed.
     * Perform total reversal.
     */
    object ONLY_TOTAL_REVERSAL_ALLOWED : ProcessingErrorEvent()

    /**
     * Product not enabled.
     * Register with required payment provider.
     */
    object PRODUCT_NOT_ENABLED : ProcessingErrorEvent()

    /**
     * Failed to generate MAC.
     * Contact technical support.
     */
    object MAC_GENERATION_FAIL : ProcessingErrorEvent()

    /**
     * Terminal not found.
     * Check terminal connection and try again.
     */
    object TERMINAL_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Error confirming transaction.
     * Try processing the sale again.
     */
    object ERROR_CONFIRMING_TRANSACTION : ProcessingErrorEvent()

    /**
     * Error opening cryptogram.
     * Contact support with transaction details.
     */
    object ERROR_OPENING_CRYPTOGRAM : ProcessingErrorEvent()

    /**
     * Use CHIP for this transaction.
     * Perform the reversal using the chip.
     */
    object USE_CHIP_FOR_TRANSACTION : ProcessingErrorEvent()

    /**
     * Operation not performed. Enter password for this operation.
     * User must enter password to proceed.
     */
    object OPERATION_NOT_PERFORMED_ENTER_PASSWORD : ProcessingErrorEvent()

    /**
     * Duplicate transaction.
     * Transaction already processed with this same amount and card.
     */
    object DUPLICATE_TRANSACTION : ProcessingErrorEvent()

    /**
     * Transaction cannot be cancelled.
     * The transaction is already completed or cannot be cancelled.
     */
    object TRANSACTION_CANNOT_BE_CANCELLED : ProcessingErrorEvent()

    /**
     * Invalid payment method.
     * A payment method that the card does not have may have been selected (debit, credit or voucher).
     */
    object INVALID_PAYMENT_METHOD : ProcessingErrorEvent()

    /**
     * Response TLV with size larger than expected.
     * Send for analysis the seller's email date time and terminal number.
     */
    object RESPONSE_TLV_SIZE : ProcessingErrorEvent()

    /**
     * Card read error.
     * Unable to read card data from the card reader.
     */
    object CARD_READ_ERROR : ProcessingErrorEvent()

    /**
     * Card read operation was canceled.
     * The card reading process was interrupted or canceled.
     */
    object CARD_READ_CANCELED : ProcessingErrorEvent()

    /**
     * Multiple cards detected during read operation.
     * Remove extra cards and try again with a single card.
     */
    object CARD_READ_MULTI_ERROR : ProcessingErrorEvent()

    /**
     * Unable to read card holder information.
     * Card data is incomplete or corrupted.
     */
    object CARD_HOLDER_READ_ERROR : ProcessingErrorEvent()

    /**
     * Pinpad connection not found.
     * Check pinpad connection and try again.
     */
    object PINPAD_CONNECTION_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Pinpad already connected.
     * Another connection to the pinpad is already active.
     */
    object PINPAD_ALREADY_CONNECTED : ProcessingErrorEvent()

    /**
     * Pinpad connection was closed.
     * The connection to the pinpad was terminated.
     */
    object PINPAD_CLOSED_CONNECTION : ProcessingErrorEvent()

    /**
     * Input/Output error with pinpad.
     * Communication error occurred during pinpad operation.
     */
    object IO_ERROR_WITH_PINPAD : ProcessingErrorEvent()

    /**
     * Transaction application is blocked.
     * The payment application on the card is blocked.
     */
    object TRANSACTION_APP_BLOCKED : ProcessingErrorEvent()

    /**
     * CVV was not provided.
     * Card verification value is required for this transaction.
     */
    object CVV_NOT_PROVIDED : ProcessingErrorEvent()

    /**
     * Invalid CVV provided.
     * The card verification value is incorrect.
     */
    object CVV_INVALID : ProcessingErrorEvent()

    /**
     * No transaction type specified.
     * Transaction type must be specified to proceed.
     */
    object NO_TRANSACTION_TYPE : ProcessingErrorEvent()

    /**
     * Wrong transaction type selected.
     * The selected transaction type is not valid for this operation.
     */
    object WRONG_TRANSACTION_TYPE : ProcessingErrorEvent()

    /**
     * Invalid application selected.
     * The payment application on the card is not valid.
     */
    object INVALID_APPLICATION : ProcessingErrorEvent()

    /**
     * Invalid application index.
     * The application index on the card is not valid.
     */
    object INVALID_APPLICATION_INDEX : ProcessingErrorEvent()

    /**
     * Online processing error.
     * Error occurred during online transaction processing.
     */
    object ONLINE_PROCESSING_ERROR : ProcessingErrorEvent()

    /**
     * EMV processing error.
     * Generic error during EMV chip card processing.
     */
    object EMV_PROCESSING_ERROR : ProcessingErrorEvent()

    /**
     * EMV card connection failed.
     * Unable to establish connection with the EMV chip.
     */
    object EMV_CARD_CONNECTION_ERROR : ProcessingErrorEvent()

    /**
     * No EMV application found.
     * No compatible payment application found on the chip card.
     */
    object EMV_NO_APPLICATION : ProcessingErrorEvent()

    /**
     * EMV CAPK error.
     * Certificate Authority Public Key error during EMV processing.
     */
    object EMV_CAPK_ERROR : ProcessingErrorEvent()

    /**
     * EMV TLV error.
     * Tag-Length-Value data structure error during EMV processing.
     */
    object EMV_TLV_ERROR : ProcessingErrorEvent()

    /**
     * EMV AID error.
     * Application Identifier error during EMV processing.
     */
    object EMV_AID_ERROR : ProcessingErrorEvent()

    /**
     * PIN entry error.
     * Error occurred during PIN entry process.
     */
    object PIN_ENTRY_ERROR : ProcessingErrorEvent()

    /**
     * PIN key error.
     * Error with PIN encryption key.
     */
    object PIN_KEY_ERROR : ProcessingErrorEvent()

    /**
     * No PIN input provided.
     * PIN entry was required but not provided.
     */
    object PIN_NO_INPUT : ProcessingErrorEvent()

    /**
     * PIN initialization error.
     * Error occurred during PIN entry system initialization.
     */
    object PIN_INITIALIZATION_ERROR : ProcessingErrorEvent()

    /**
     * PIN encryption error.
     * Error occurred during PIN encryption process.
     */
    object PIN_ENCRYPTION_ERROR : ProcessingErrorEvent()

    /**
     * PIN key not found.
     * Required PIN encryption key was not found.
     */
    object PIN_KEY_NOT_FOUND : ProcessingErrorEvent()

    /**
     * NFC not supported.
     * Near Field Communication is not supported on this device.
     */
    object NFC_NOT_SUPPORTED : ProcessingErrorEvent()

    /**
     * NFC operation aborted.
     * The NFC operation was aborted or interrupted.
     */
    object NFC_OPERATION_ABORTED : ProcessingErrorEvent()

    /**
     * Wrong card type for NFC.
     * The card type is not compatible with NFC operations.
     */
    object NFC_WRONG_CARD_TYPE : ProcessingErrorEvent()

    /**
     * Invalid NFC key.
     * The NFC authentication key is invalid.
     */
    object NFC_INVALID_KEY : ProcessingErrorEvent()

    /**
     * NFC not authenticated.
     * NFC authentication failed.
     */
    object NFC_NOT_AUTHENTICATED : ProcessingErrorEvent()

    /**
     * Invalid sector number.
     * The specified sector number is not valid.
     */
    object INVALID_SECTOR_NUMBER : ProcessingErrorEvent()

    /**
     * Invalid block number.
     * The specified block number is not valid.
     */
    object INVALID_BLOCK_NUMBER : ProcessingErrorEvent()

    /**
     * Invalid block format.
     * The data block format is not valid.
     */
    object INVALID_BLOCK_FORMAT : ProcessingErrorEvent()

    /**
     * QR code generation error.
     * Failed to generate QR code.
     */
    object QRCODE_GENERATION_ERROR : ProcessingErrorEvent()

    /**
     * QR code expired.
     * The QR code has expired and is no longer valid.
     */
    object QRCODE_EXPIRED : ProcessingErrorEvent()

    /**
     * NFC cart read error.
     * Failed to read cart data from NFC tag.
     */
    object NFC_CART_READ_ERROR : ProcessingErrorEvent()

    /**
     * NFC cart write error.
     * Failed to write cart data to NFC tag.
     */
    object NFC_CART_WRITE_ERROR : ProcessingErrorEvent()

    /**
     * NFC cart header invalid.
     * The cart header on the NFC tag is invalid or corrupted.
     */
    object NFC_CART_HEADER_INVALID : ProcessingErrorEvent()

    /**
     * NFC insufficient space.
     * Not enough space on the NFC tag for the requested operation.
     */
    object NFC_INSUFFICIENT_SPACE : ProcessingErrorEvent()

    /**
     * NFC cart item not found.
     * The specified cart item was not found on the tag.
     */
    object NFC_CART_ITEM_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Transaction fallback initiated.
     * Transaction is falling back to alternative processing method.
     */
    object TRANSACTION_FALLBACK : ProcessingErrorEvent()

    /**
     * Invalid card mode.
     * The card mode is not valid for this transaction type.
     */
    object INVALID_CARD_MODE : ProcessingErrorEvent()

    /**
     * Too many cards detected.
     * Multiple cards detected, use only one card.
     */
    object TOO_MANY_CARDS : ProcessingErrorEvent()

    /**
     * Device misconfigured.
     * The device configuration is incorrect.
     */
    object DEVICE_MISCONFIGURED : ProcessingErrorEvent()

    /**
     * Activation error.
     * Error occurred during device or service activation.
     */
    object ACTIVATION_ERROR : ProcessingErrorEvent()

    /**
     * SDK version outdated.
     * The SDK version is outdated and needs to be updated.
     */
    object SDK_VERSION_OUTDATED : ProcessingErrorEvent()

    /**
     * Application name not set.
     * Application name must be configured before use.
     */
    object APP_NAME_NOT_SET : ProcessingErrorEvent()

    /**
     * No active application.
     * No application is currently active or running.
     */
    object NO_ACTIVE_APPLICATION : ProcessingErrorEvent()

    /**
     * Multiple provider instances running.
     * Only one provider instance should be active at a time.
     */
    object MULTIPLE_PROVIDER_INSTANCES : ProcessingErrorEvent()

    /**
     * Unknown user type.
     * The user type is not recognized or supported.
     */
    object UNKNOWN_USER_TYPE : ProcessingErrorEvent()

    /**
     * Transaction object is null.
     * The transaction object is null or not initialized.
     */
    object TRANSACTION_OBJECT_NULL : ProcessingErrorEvent()

    /**
     * Email error.
     * Error occurred during email operation.
     */
    object EMAIL_ERROR : ProcessingErrorEvent()

    /**
     * Email client error.
     * Error with the email client configuration or operation.
     */
    object EMAIL_CLIENT_ERROR : ProcessingErrorEvent()

    /**
     * Email address is empty.
     * Email address field is required but empty.
     */
    object EMAIL_EMPTY : ProcessingErrorEvent()

    /**
     * Email recipient is empty.
     * Email recipient field is required but empty.
     */
    object EMAIL_RECIPIENT_EMPTY : ProcessingErrorEvent()

    /**
     * Data constraint error.
     * Data does not meet required constraints.
     */
    object DATA_CONSTRAINT_ERROR : ProcessingErrorEvent()

    /**
     * Data integration error.
     * Error occurred during data integration process.
     */
    object DATA_INTEGRATION_ERROR : ProcessingErrorEvent()

    /**
     * Switch interface error.
     * Error occurred in the switch interface communication.
     */
    object SWITCH_INTERFACE_ERROR : ProcessingErrorEvent()

    /**
     * Swipe incorrect.
     * Card was not swiped correctly, try again.
     */
    object SWIPE_INCORRECT : ProcessingErrorEvent()

    /**
     * Null response received.
     * Expected response was null or empty.
     */
    object NULL_RESPONSE : ProcessingErrorEvent()

    /**
     * Error response received.
     * Received an error response from the system.
     */
    object ERROR_RESPONSE : ProcessingErrorEvent()

    /**
     * Card was removed by user.
     * The card was removed during processing.
     */
    object CARD_REMOVED_BY_USER : ProcessingErrorEvent()

    /**
     * Cannot read chip card.
     * Unable to read data from the chip card.
     */
    object CHIP_CARD_READ_ERROR : ProcessingErrorEvent()

    /**
     * Generic card error.
     * A general card-related error occurred.
     */
    object CARD_GENERIC_ERROR : ProcessingErrorEvent()

    /**
     * Device not compatible.
     * The device is not compatible with the operation.
     */
    object DEVICE_NOT_COMPATIBLE : ProcessingErrorEvent()

    /**
     * Magnetic stripe passed but chip card detected.
     * Card has a chip but was swiped instead of inserted.
     */
    object MAG_STRIPE_CHIP_DETECTED : ProcessingErrorEvent()

    /**
     * Invalid Stone code.
     * The Stone authentication code is invalid.
     */
    object INVALID_STONE_CODE : ProcessingErrorEvent()

    /**
     * User model not found.
     * The user model could not be found in the system.
     */
    object USER_MODEL_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Invalid or unknown Stone code.
     * The Stone code is invalid or not recognized.
     */
    object INVALID_OR_UNKNOWN_STONE_CODE : ProcessingErrorEvent()

    /**
     * Printer initialization error.
     * Error occurred during printer initialization.
     */
    object PRINTER_INITIALIZATION_ERROR : ProcessingErrorEvent()

    /**
     * Printer unsupported format.
     * The printer does not support the requested format.
     */
    object PRINTER_UNSUPPORTED_FORMAT : ProcessingErrorEvent()

    /**
     * Printer invalid data.
     * The data provided to the printer is invalid.
     */
    object PRINTER_INVALID_DATA : ProcessingErrorEvent()

    /**
     * No print support.
     * Printing is not supported on this device.
     */
    object NO_PRINT_SUPPORT : ProcessingErrorEvent()

    /**
     * Internal system error.
     * An internal error occurred in the system.
     */
    object INTERNAL_SYSTEM_ERROR : ProcessingErrorEvent()

    /**
     * Tables not found.
     * Required data tables were not found.
     */
    object TABLES_NOT_FOUND : ProcessingErrorEvent()

    /**
     * Need to load tables.
     * Data tables need to be loaded before proceeding.
     */
    object NEED_LOAD_TABLES : ProcessingErrorEvent()

    /**
     * Invalid acquirer activation code.
     * The activation code provided for the acquirer is invalid.
     */
    object INVALID_ACQUIRER_ACTIVATION_CODE : ProcessingErrorEvent()

    /**
     * Reversal pending.
     * The transaction is pending reversal.
     */
    object REVERSAL_PENDING : ProcessingErrorEvent()

    /**
     * Transaction is pending.
     * The transaction is currently pending and not yet completed.
     */
    object TRANSACTION_PENDING : ProcessingErrorEvent()

    /**
     * Transaction cancelled and awaiting reversal.
     * The transaction was cancelled and is awaiting reversal processing.
     */
    object CANCELLED_AWAITING_REVERSAL : ProcessingErrorEvent()

    /**
     * Fraud suspicion detected.
     */
    object FRAUD_SUSPICION : ProcessingErrorEvent()

    /**
     * Check card details.
     */
    object CHECK_CARD_DETAILS : ProcessingErrorEvent()

    /**
     * Use credit card method.
     */
    object USE_CREDIT_METHOD : ProcessingErrorEvent()

    /**
     * Use debit card method.
     */
    object USE_DEBIT_METHOD : ProcessingErrorEvent()

    /**
     * Check special conditions.
     */
    object CHECK_SPECIAL_CONDITIONS : ProcessingErrorEvent()

    /**
     * Approval after identity verification is required.
     */
    object APPROVE_AFTER_IDENTITY_VERIFICATION : ProcessingErrorEvent()

    /**
     * Card error.
     */
    object UNACCEPTABLE_FEE : ProcessingErrorEvent()

    /**
     * Card with restriction.
     */
    object CARD_WITH_RESTRICTION : ProcessingErrorEvent()

    /**
     * Exceeded password attempts.
     */
    object EXCEEDED_PASSWORD_ATTEMPTS : ProcessingErrorEvent()

    /**
     * Card was lost.
     */
    object LOST_CARD : ProcessingErrorEvent()

    /**
     * Card was stolen.
     */
    object STOLEN_CARD : ProcessingErrorEvent()

    /**
     * Exceeded health value limit.
     */
    object EXCEEDED_HEALTH_VALUE_LIMIT : ProcessingErrorEvent()

    /**
     * Exceeded withdrawal quantity limit.
     */
    object EXCEEDED_WITHDRAWAL_QUANTITY_LIMIT : ProcessingErrorEvent()

    /**
     * Cutover in process.
     */
    object CUTOVER_IN_PROCESS : ProcessingErrorEvent()

    /**
     * Violation of law.
     */
    object VIOLATION_OF_LAW : ProcessingErrorEvent()

    /**
     * Reconciliation error.
     */
    object RECONCILIATION_ERROR : ProcessingErrorEvent()

    /**
     * Poor status destination.
     */
    object POOR_STATUS_DESTINATION : ProcessingErrorEvent()

    /**
     * Poor status origin.
     */
    object POOR_STATUS_ORIGIN : ProcessingErrorEvent()

    /**
     * Rejected key verification failed.
     */
    object REJECTED_KEY_VERIFICATION_FAILED : ProcessingErrorEvent()

    /**
     * Issuer unavailable.
     */
    object ISSUER_UNAVAILABLE : ProcessingErrorEvent()

    /**
     * Invalid life cycle.
     */
    object INVALID_LIFE_CYCLE : ProcessingErrorEvent()

    /**
     * Unblock the card.
     */
    object UNBLOCK_THE_CARD : ProcessingErrorEvent()
}
