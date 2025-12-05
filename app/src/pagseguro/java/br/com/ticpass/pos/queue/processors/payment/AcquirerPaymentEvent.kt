package br.com.ticpass.pos.core.queue.processors.payment

import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEvent
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData

enum class AcquirerPaymentEvent(val code: Int, val event: PaymentProcessingEvent) {
    /**
     * User should insert or reach the card.
     */
    CARD_REACH_OR_INSERT(
        PlugPagEventData.Companion.EVENT_CODE_WAITING_CARD,
        PaymentProcessingEvent.CARD_REACH_OR_INSERT
    ),

    /**
     * Transaction has been approved.
     */
    APPROVAL_SUCCEEDED(
        PlugPagEventData.Companion.EVENT_CODE_SALE_APPROVED,
        PaymentProcessingEvent.APPROVAL_SUCCEEDED
    ),
    /**
     * Transaction has been declined.
     */
    APPROVAL_DECLINED(
        PlugPagEventData.Companion.EVENT_CODE_SALE_NOT_APPROVED,
        PaymentProcessingEvent.APPROVAL_DECLINED
    ),

    /**
     * Transaction has been successfully completed.
     */
    TRANSACTION_DONE(
        PlugPagEventData.Companion.EVENT_CODE_SALE_END,
        PaymentProcessingEvent.TRANSACTION_DONE
    ),
    /**
     * Transaction is being processed.
     */
    TRANSACTION_PROCESSING_1(
        PlugPagEventData.Companion.EVENT_CODE_DEFAULT,
        PaymentProcessingEvent.TRANSACTION_PROCESSING
    ),
    /**
     * Transaction is being processed.
     */
    TRANSACTION_PROCESSING_2(
        PlugPagEventData.Companion.EVENT_CODE_CUSTOM_MESSAGE,
        PaymentProcessingEvent.TRANSACTION_PROCESSING
    ),

    /**
     * Transaction is going through authorization phase.
     */
    AUTHORIZING(
        PlugPagEventData.Companion.EVENT_CODE_AUTHORIZING,
        PaymentProcessingEvent.AUTHORIZING
    ),

    /**
    * Card bin information is getting verified.
    */
    CARD_BIN_VERIFYING(
        PlugPagEventData.Companion.EVENT_CODE_CAR_BIN_REQUESTED,
        PaymentProcessingEvent.CARD_BIN_REQUESTED
    ),

    /**
     * Card bin information has been checked.
     */
    CARD_BIN_OK(
        PlugPagEventData.Companion.EVENT_CODE_CAR_BIN_OK,
        PaymentProcessingEvent.CARD_BIN_OK
    ),

    /**
    * Card holder information is getting verified.
    */
    CARD_HOLDER_VERIFYING(
        PlugPagEventData.Companion.EVENT_CODE_CAR_HOLDER_REQUESTED,
        PaymentProcessingEvent.CARD_HOLDER_REQUESTED
    ),

    /**
    * Card holder information has been checked.
    */
    CARD_HOLDER_OK(
        PlugPagEventData.Companion.EVENT_CODE_CAR_HOLDER_OK,
        PaymentProcessingEvent.CARD_HOLDER_OK
    ),

    /**
    * Contactless payment has failed.
    */
    CONTACTLESS_ERROR(
        PlugPagEventData.Companion.EVENT_CODE_CONTACTLESS_ERROR,
        PaymentProcessingEvent.CONTACTLESS_ERROR
    ),

    /**
    * Contactless payment has been detected.
    */
    CONTACTLESS_ON_DEVICE(
        PlugPagEventData.Companion.EVENT_CODE_CONTACTLESS_ON_DEVICE,
        PaymentProcessingEvent.CONTACTLESS_ON_DEVICE
    ),

    /**
     * CVV information has been checked.
     */
    CVV_OK(
        PlugPagEventData.Companion.EVENT_CODE_CVV_OK,
        PaymentProcessingEvent.CVV_OK
    ),
    /**
     * CVV information is getting verified.
     */
    CVV_VERIFYING(
        PlugPagEventData.Companion.EVENT_CODE_CVV_REQUESTED,
        PaymentProcessingEvent.CVV_REQUESTED,
    ),

    /**
     * Downloading tables for transaction processing.
     */
    DOWNLOADING_TABLES(
        PlugPagEventData.Companion.EVENT_CODE_DOWNLOADING_TABLES,
        PaymentProcessingEvent.DOWNLOADING_TABLES
    ),
    /**
     * Saving tables for transaction processing.
     */
    SAVING_TABLES(
        PlugPagEventData.Companion.EVENT_CODE_RECORDING_TABLES,
        PaymentProcessingEvent.SAVING_TABLES
    ),

    /**
     * Generic success event.
     */
    GENERIC_SUCCESS(
        PlugPagEventData.Companion.EVENT_CODE_SUCCESS,
        PaymentProcessingEvent.GENERIC_SUCCESS
    ),
    /**
     * Generic error event.
     */
    GENERIC_ERROR(
        PlugPagEventData.Companion.ON_EVENT_ERROR,
        PaymentProcessingEvent.GENERIC_ERROR
    ),

    /**
     * User should insert the card.
     */
    USE_CHIP(
        PlugPagEventData.Companion.EVENT_CODE_USE_CHIP,
        PaymentProcessingEvent.USE_CHIP
    ),
    /**
     * User should insert the card with a magnetic stripe.
     */
    USE_MAGNETIC_STRIPE(
        PlugPagEventData.Companion.EVENT_CODE_USE_TARJA,
        PaymentProcessingEvent.USE_MAGNETIC_STRIPE
    ),

    /**
     * Card has been inserted.
     */
    CARD_INSERTED(
        PlugPagEventData.Companion.EVENT_CODE_INSERTED_CARD,
        PaymentProcessingEvent.CARD_INSERTED
    ),
    /**
     * User should remove the card.
     */
    CARD_REMOVAL_REQUESTING(
        PlugPagEventData.Companion.EVENT_CODE_WAITING_REMOVE_CARD,
        PaymentProcessingEvent.CARD_REMOVAL_REQUESTING
    ),
    /**
     * User has removed the card.
     */
    CARD_REMOVAL_SUCCEEDED(
        PlugPagEventData.Companion.EVENT_CODE_REMOVED_CARD,
        PaymentProcessingEvent.CARD_REMOVAL_SUCCEEDED
    ),

    /**
     * Key has been inserted.
     */
    KEY_INSERTED(
        PlugPagEventData.Companion.EVENT_CODE_INSERTED_KEY,
        PaymentProcessingEvent.KEY_INSERTED
    ),
    /**
     * Activation has succeeded.
     */
    ACTIVATION_SUCCEEDED(
        PlugPagEventData.Companion.EVENT_CODE_ACTIVATION_SUCCESS,
        PaymentProcessingEvent.ACTIVATION_SUCCEEDED
    ),

    /**
     * Solving pending issues during payment processing.
     */
    SOLVING_PENDING_ISSUES(
        PlugPagEventData.Companion.EVENT_CODE_SOLVE_PENDINGS,
        PaymentProcessingEvent.SOLVING_PENDING_ISSUES
    ),

    /**
     * User should input their card PIN.
     */
    PIN_REQUESTED(
        PlugPagEventData.Companion.EVENT_CODE_PIN_REQUESTED,
        PaymentProcessingEvent.PIN_REQUESTED
    ),
    /**
     * User has input 1 PIN digit.
     */
    PIN_DIGIT_INPUT(
        PlugPagEventData.Companion.EVENT_CODE_DIGIT_PASSWORD,
        PaymentProcessingEvent.PIN_DIGIT_INPUT
    ),
    /**
     * User has deleted 1 PIN digit.
     */
    PIN_DIGIT_REMOVED(
        PlugPagEventData.Companion.EVENT_CODE_NO_PASSWORD,
        PaymentProcessingEvent.PIN_DIGIT_REMOVED
    ),
    /**
     * User card PIN has been verified.
     */
    PIN_OK(
        PlugPagEventData.Companion.EVENT_CODE_PIN_OK,
        PaymentProcessingEvent.PIN_OK
    );

    companion object {
        fun translate(code: Int): PaymentProcessingEvent {
            val event = entries.find { it.code == code }?.event
            return event ?: throw IllegalArgumentException("Unknown code: $code")
        }

        fun translate(event: PaymentProcessingEvent): Int {
            val code = entries.find { it.event == event }?.code
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}