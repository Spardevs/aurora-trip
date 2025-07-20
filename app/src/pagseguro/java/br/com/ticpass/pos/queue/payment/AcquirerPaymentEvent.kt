package br.com.ticpass.pos.queue.payment

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData

enum class AcquirerPaymentEvent(val code: Int, val event: ProcessingPaymentEvent) {
    /**
     * User should insert or reach the card.
     */
    CARD_REACH_OR_INSERT(
        PlugPagEventData.Companion.EVENT_CODE_WAITING_CARD,
        ProcessingPaymentEvent.CARD_REACH_OR_INSERT
    ),

    /**
     * Transaction has been approved.
     */
    APPROVAL_SUCCEEDED(
        PlugPagEventData.Companion.EVENT_CODE_SALE_APPROVED,
        ProcessingPaymentEvent.APPROVAL_SUCCEEDED
    ),
    /**
     * Transaction has been declined.
     */
    APPROVAL_DECLINED(
        PlugPagEventData.Companion.EVENT_CODE_SALE_NOT_APPROVED,
        ProcessingPaymentEvent.APPROVAL_DECLINED
    ),

    /**
     * Transaction has been successfully completed.
     */
    TRANSACTION_DONE(
        PlugPagEventData.Companion.EVENT_CODE_SALE_END,
        ProcessingPaymentEvent.TRANSACTION_DONE
    ),
    /**
     * Transaction is being processed.
     */
    TRANSACTION_PROCESSING_1(
        PlugPagEventData.Companion.EVENT_CODE_DEFAULT,
        ProcessingPaymentEvent.TRANSACTION_PROCESSING
    ),
    /**
     * Transaction is being processed.
     */
    TRANSACTION_PROCESSING_2(
        PlugPagEventData.Companion.EVENT_CODE_CUSTOM_MESSAGE,
        ProcessingPaymentEvent.TRANSACTION_PROCESSING
    ),

    /**
     * Transaction is going through authorization phase.
     */
    AUTHORIZING(
        PlugPagEventData.Companion.EVENT_CODE_AUTHORIZING,
        ProcessingPaymentEvent.AUTHORIZING
    ),

    /**
    * Card bin information is getting verified.
    */
    CARD_BIN_VERIFYING(
        PlugPagEventData.Companion.EVENT_CODE_CAR_BIN_REQUESTED,
        ProcessingPaymentEvent.CARD_BIN_REQUESTED
    ),

    /**
     * Card bin information has been checked.
     */
    CARD_BIN_OK(
        PlugPagEventData.Companion.EVENT_CODE_CAR_BIN_OK,
        ProcessingPaymentEvent.CARD_BIN_OK
    ),

    /**
    * Card holder information is getting verified.
    */
    CARD_HOLDER_VERIFYING(
        PlugPagEventData.Companion.EVENT_CODE_CAR_HOLDER_REQUESTED,
        ProcessingPaymentEvent.CARD_HOLDER_REQUESTED
    ),

    /**
    * Card holder information has been checked.
    */
    CARD_HOLDER_OK(
        PlugPagEventData.Companion.EVENT_CODE_CAR_HOLDER_OK,
        ProcessingPaymentEvent.CARD_HOLDER_OK
    ),

    /**
    * Contactless payment has failed.
    */
    CONTACTLESS_ERROR(
        PlugPagEventData.Companion.EVENT_CODE_CONTACTLESS_ERROR,
        ProcessingPaymentEvent.CONTACTLESS_ERROR
    ),

    /**
    * Contactless payment has been detected.
    */
    CONTACTLESS_ON_DEVICE(
        PlugPagEventData.Companion.EVENT_CODE_CONTACTLESS_ON_DEVICE,
        ProcessingPaymentEvent.CONTACTLESS_ON_DEVICE
    ),

    /**
     * CVV information has been checked.
     */
    CVV_OK(
        PlugPagEventData.Companion.EVENT_CODE_CVV_OK,
        ProcessingPaymentEvent.CVV_OK
    ),
    /**
     * CVV information is getting verified.
     */
    CVV_VERIFYING(
        PlugPagEventData.Companion.EVENT_CODE_CVV_REQUESTED,
        ProcessingPaymentEvent.CVV_REQUESTED,
    ),

    /**
     * Downloading tables for transaction processing.
     */
    DOWNLOADING_TABLES(
        PlugPagEventData.Companion.EVENT_CODE_DOWNLOADING_TABLES,
        ProcessingPaymentEvent.DOWNLOADING_TABLES
    ),
    /**
     * Saving tables for transaction processing.
     */
    SAVING_TABLES(
        PlugPagEventData.Companion.EVENT_CODE_RECORDING_TABLES,
        ProcessingPaymentEvent.SAVING_TABLES
    ),

    /**
     * Generic success event.
     */
    GENERIC_SUCCESS(
        PlugPagEventData.Companion.EVENT_CODE_SUCCESS,
        ProcessingPaymentEvent.GENERIC_SUCCESS
    ),
    /**
     * Generic error event.
     */
    GENERIC_ERROR(
        PlugPagEventData.Companion.ON_EVENT_ERROR,
        ProcessingPaymentEvent.GENERIC_ERROR
    ),

    /**
     * User should insert the card.
     */
    USE_CHIP(
        PlugPagEventData.Companion.EVENT_CODE_USE_CHIP,
        ProcessingPaymentEvent.USE_CHIP
    ),
    /**
     * User should insert the card with a magnetic stripe.
     */
    USE_MAGNETIC_STRIPE(
        PlugPagEventData.Companion.EVENT_CODE_USE_TARJA,
        ProcessingPaymentEvent.USE_MAGNETIC_STRIPE
    ),

    /**
     * Card has been inserted.
     */
    CARD_INSERTED(
        PlugPagEventData.Companion.EVENT_CODE_INSERTED_CARD,
        ProcessingPaymentEvent.CARD_INSERTED
    ),
    /**
     * User should remove the card.
     */
    CARD_REMOVAL_REQUESTING(
        PlugPagEventData.Companion.EVENT_CODE_WAITING_REMOVE_CARD,
        ProcessingPaymentEvent.CARD_REMOVAL_REQUESTING
    ),
    /**
     * User has removed the card.
     */
    CARD_REMOVAL_SUCCEEDED(
        PlugPagEventData.Companion.EVENT_CODE_REMOVED_CARD,
        ProcessingPaymentEvent.CARD_REMOVAL_SUCCEEDED
    ),

    /**
     * Key has been inserted.
     */
    KEY_INSERTED(
        PlugPagEventData.Companion.EVENT_CODE_INSERTED_KEY,
        ProcessingPaymentEvent.KEY_INSERTED
    ),
    /**
     * Activation has succeeded.
     */
    ACTIVATION_SUCCEEDED(
        PlugPagEventData.Companion.EVENT_CODE_ACTIVATION_SUCCESS,
        ProcessingPaymentEvent.ACTIVATION_SUCCEEDED
    ),

    /**
     * Solving pending issues during payment processing.
     */
    SOLVING_PENDING_ISSUES(
        PlugPagEventData.Companion.EVENT_CODE_SOLVE_PENDINGS,
        ProcessingPaymentEvent.SOLVING_PENDING_ISSUES
    ),

    /**
     * User should input their card PIN.
     */
    PIN_REQUESTED(
        PlugPagEventData.Companion.EVENT_CODE_PIN_REQUESTED,
        ProcessingPaymentEvent.PIN_REQUESTED
    ),
    /**
     * User has input 1 PIN digit.
     */
    PIN_DIGIT_INPUT(
        PlugPagEventData.Companion.EVENT_CODE_DIGIT_PASSWORD,
        ProcessingPaymentEvent.PIN_DIGIT_INPUT
    ),
    /**
     * User has deleted 1 PIN digit.
     */
    PIN_DIGIT_REMOVED(
        PlugPagEventData.Companion.EVENT_CODE_NO_PASSWORD,
        ProcessingPaymentEvent.PIN_DIGIT_REMOVED
    ),
    /**
     * User card PIN has been verified.
     */
    PIN_OK(
        PlugPagEventData.Companion.EVENT_CODE_PIN_OK,
        ProcessingPaymentEvent.PIN_OK
    );

    companion object {
        fun translate(code: Int): ProcessingPaymentEvent {
            val event = entries.find { it.code == code }?.event
            return event ?: throw IllegalArgumentException("Unknown code: $code")
        }

        fun translate(event: ProcessingPaymentEvent): Int {
            val code = entries.find { it.event == event }?.code
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}