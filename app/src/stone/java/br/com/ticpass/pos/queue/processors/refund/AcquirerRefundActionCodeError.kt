package br.com.ticpass.pos.core.queue.processors.refund

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent

enum class AcquirerRefundActionCodeError(val actionCode: String, val event: ProcessingErrorEvent) {
    INVALID_AMOUNT_1(
        "1007",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    INVALID_AMOUNT_2(
        "1809",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    INVALID_AMOUNT_3(
        "1815",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    TRANSACTION_NOT_ALLOWED_1(
        "2802",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),

    TRANSACTION_NOT_ALLOWED_2(
        "1836",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),

    TRANSACTION_NOT_ALLOWED_3(
        "1020",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),

    TRANSACTION_NOT_ALLOWED_4(
        "1019",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),

    TRANSACTION_AMOUNT_NOT_ALLOWED_1(
        "1804",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    TRANSACTION_AMOUNT_NOT_ALLOWED_2(
        "1804",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    TRANSACTION_AMOUNT_NOT_ALLOWED_3(
        "1010",
        ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
    ),

    CONTACT_CREDIT_CARD_CENTER_1(
        "2000",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_2(
        "1836",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_3(
        "1000",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_4(
        "1820",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_5(
        "1838",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_6(
        "1830",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_7(
        "3002",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_8(
        "3005",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_9(
        "1813",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_10(
        "1828",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CONTACT_CREDIT_CARD_CENTER_11(
        "1833",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    INVALID_INSTALLMENT_1(
        "1832",
        ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT
    ),

    INVALID_INSTALLMENT_2(
        "1805",
        ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT
    ),

    INVALID_INSTALLMENT_3(
        "1805",
        ProcessingErrorEvent.INVALID_INSTALLMENT_AMOUNT
    ),

    INVALID_PASSWORD_1(
        "1017",
        ProcessingErrorEvent.CHECK_PASSWORD
    ),

    INVALID_PASSWORD_2(
        "1809",
        ProcessingErrorEvent.CHECK_PASSWORD
    ),

    INVALID_PASSWORD_3(
        "1815",
        ProcessingErrorEvent.CHECK_PASSWORD
    ),

    INVALID_PASSWORD_4(
        "1048",
        ProcessingErrorEvent.CHECK_PASSWORD
    ),

    INVALID_PASSWORD_5(
        "1827",
        ProcessingErrorEvent.CHECK_PASSWORD
    ),

    FRAUD_SUSPICION_1(
        "2002",
        ProcessingErrorEvent.FRAUD_SUSPICION
    ),

    FRAUD_SUSPICION_2(
        "1002",
        ProcessingErrorEvent.FRAUD_SUSPICION
    ),

    COMMUNICATION_FAILURE_1(
        "1831",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    COMMUNICATION_FAILURE_2(
        "1829",
        ProcessingErrorEvent.COMMUNICATION_ERROR
    ),

    CHECK_CARD_DETAILS_1(
        "1819",
        ProcessingErrorEvent.CHECK_CARD_DETAILS
    ),

    CHECK_CARD_DETAILS_2(
        "1022",
        ProcessingErrorEvent.CHECK_CARD_DETAILS
    ),

    CHECK_CARD_DETAILS_3(
        "1817",
        ProcessingErrorEvent.CHECK_CARD_DETAILS
    ),

    CHECK_CARD_DETAILS_4(
        "1816",
        ProcessingErrorEvent.CHECK_CARD_DETAILS
    ),

    CHECK_CARD_DETAILS_5(
        "1820",
        ProcessingErrorEvent.CHECK_CARD_DETAILS
    ),

    USE_CREDIT_METHOD(
        "1810",
        ProcessingErrorEvent.USE_CREDIT_METHOD
    ),

    USE_DEBIT_METHOD(
        "1811",
        ProcessingErrorEvent.USE_DEBIT_METHOD
    ),

    CHECK_SPECIAL_CONDITIONS(
        "1008",
        ProcessingErrorEvent.CHECK_SPECIAL_CONDITIONS
    ),

    UNSPECIFIED_ERROR(
        "9999",
        ProcessingErrorEvent.GENERIC
    ),

    APPROVE_AFTER_IDENTITY_VERIFICATION(
        "0001",
        ProcessingErrorEvent.APPROVE_AFTER_IDENTITY_VERIFICATION
    ),

    CARD_ERROR(
        "1820",
        ProcessingErrorEvent.CARD_READ_ERROR
    ),

    UNACCEPTABLE_FEE(
        "1013",
        ProcessingErrorEvent.UNACCEPTABLE_FEE
    ),

    MESSAGE_FORMAT_ERROR(
        "9100",
        ProcessingErrorEvent.ERROR_RESPONSE_MESSAGE_VALIDATION
    ),

    EXPIRED_CARD(
        "2001",
        ProcessingErrorEvent.EXPIRED_CARD
    ),

    ESTABLISHMENT_CONTACT_1(
        "2817",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    ESTABLISHMENT_CONTACT_2(
        "2005",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    ESTABLISHMENT_CONTACT_3(
        "2801",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED_CONTACT_MANAGER
    ),

    CARD_WITH_RESTRICTION(
        "2004",
        ProcessingErrorEvent.CARD_WITH_RESTRICTION
    ),

    EXCEEDED_PASSWORD_ATTEMPTS(
        "2006",
        ProcessingErrorEvent.EXCEEDED_PASSWORD_ATTEMPTS
    ),

    LOST_CARD(
        "2008",
        ProcessingErrorEvent.LOST_CARD
    ),

    STOLEN_CARD(
        "2009",
        ProcessingErrorEvent.STOLEN_CARD
    ),

    NOT_AUTHORIZED(
        "1016",
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),

    EXCEEDED_HEALTH_VALUE_LIMIT(
        "1021",
        ProcessingErrorEvent.EXCEEDED_HEALTH_VALUE_LIMIT
    ),

    EXCEEDED_WITHDRAWAL_QUANTITY_LIMIT(
        "1023",
        ProcessingErrorEvent.EXCEEDED_WITHDRAWAL_QUANTITY_LIMIT
    ),

    CUTOVER_IN_PROCESS(
        "9106",
        ProcessingErrorEvent.CUTOVER_IN_PROCESS
    ),

    VIOLATION_OF_LAW(
        "1024",
        ProcessingErrorEvent.VIOLATION_OF_LAW
    ),

    RECONCILIATION_ERROR(
        "9115",
        ProcessingErrorEvent.RECONCILIATION_ERROR
    ),

    NO_CARD_RECORD(
        "1018",
        ProcessingErrorEvent.CARD_NOT_IDENTIFIED
    ),

    POOR_STATUS_DESTINATION(
        "1042",
        ProcessingErrorEvent.POOR_STATUS_DESTINATION
    ),

    POOR_STATUS_ORIGIN(
        "1041",
        ProcessingErrorEvent.POOR_STATUS_ORIGIN
    ),

    REJECTED_KEY_VERIFICATION_FAILED(
        "8002",
        ProcessingErrorEvent.REJECTED_KEY_VERIFICATION_FAILED
    ),

    ISSUER_UNAVAILABLE(
        "9112",
        ProcessingErrorEvent.ISSUER_UNAVAILABLE
    ),

    INVALID_LIFE_CYCLE(
        "2810",
        ProcessingErrorEvent.INVALID_LIFE_CYCLE
    ),

    UNBLOCK_THE_CARD(
        "1025",
        ProcessingErrorEvent.UNBLOCK_THE_CARD
    );

    companion object {
        fun translate(actionCode: String): ProcessingErrorEvent {
            val event = entries.find { it.actionCode == actionCode }?.event
            return event ?: throw IllegalArgumentException("Unknown action: $actionCode")
        }

        fun translate(event: ProcessingErrorEvent): String {
            val code = entries.find { it.event == event }?.actionCode
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}