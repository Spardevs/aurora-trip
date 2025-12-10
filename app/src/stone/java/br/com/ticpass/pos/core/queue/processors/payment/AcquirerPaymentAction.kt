package br.com.ticpass.pos.core.queue.processors.payment

import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEvent
import stone.application.enums.Action

enum class AcquirerPaymentAction(val action: Action, val event: PaymentProcessingEvent) {
    /**
     * User should insert or reach the card.
     */
    CARD_REACH_OR_INSERT(
        Action.TRANSACTION_WAITING_CARD,
        PaymentProcessingEvent.CARD_REACH_OR_INSERT
    ),

    /**
     * User should swipe the card.
     */
    TRANSACTION_WAITING_SWIPE_CARD(
        Action.TRANSACTION_WAITING_SWIPE_CARD,
        PaymentProcessingEvent.SWIPE_CARD_REQUESTED,
    ),

    /**
     * User should scan the QR code (usually PIX).
     */
    TRANSACTION_WAITING_QRCODE_SCAN(
        Action.TRANSACTION_WAITING_QRCODE_SCAN,
        PaymentProcessingEvent.QRCODE_SCAN(),
    ),

    /**
     * User should input the card PIN.
     */
    TRANSACTION_WAITING_PASSWORD(
        Action.TRANSACTION_WAITING_PASSWORD,
        PaymentProcessingEvent.PIN_REQUESTED,
    ),

    /**
     * Transaction data is being sent over the network.
     */
    TRANSACTION_SENDING(
        Action.TRANSACTION_SENDING,
        PaymentProcessingEvent.TRANSACTION_PROCESSING,
    ),

    /**
     * Transaction with error is being reversed.
     */
    REVERSING_TRANSACTION_WITH_ERROR(
        Action.REVERSING_TRANSACTION_WITH_ERROR,
        PaymentProcessingEvent.REVERSING_TRANSACTION_WITH_ERROR,
    ),

    /**
     * User should remove the card.
     */
    TRANSACTION_REMOVE_CARD(
        Action.TRANSACTION_REMOVE_CARD,
        PaymentProcessingEvent.CARD_REMOVAL_REQUESTING,
    ),

    /**
     * Card has been removed.
     */
    TRANSACTION_CARD_REMOVED(
        Action.TRANSACTION_CARD_REMOVED,
        PaymentProcessingEvent.CARD_REMOVAL_SUCCEEDED,
    ),

    /**
     * Selection of payment method is required.
     */
    TRANSACTION_TYPE_SELECTION(
        Action.TRANSACTION_TYPE_SELECTION,
        PaymentProcessingEvent.SELECT_PAYMENT_METHOD,
    ),

    /**
     * User needs to check the device for transaction approval.
     */
    TRANSACTION_REQUIRES_CARDHOLDER_TO_CHECK_DEVICE(
        Action.TRANSACTION_REQUIRES_CARDHOLDER_TO_CHECK_DEVICE,
        PaymentProcessingEvent.CARD_HOLDER_REQUESTED,
    ),

    /**
     * Switch payment interface.
     */
    SWITCH_INTERFACE(
        Action.SWITCH_INTERFACE,
        PaymentProcessingEvent.SWITCH_INTERFACE,
    );

    companion object {
        fun translate(action: Action): PaymentProcessingEvent {
            val event = entries.find { it.action == action }?.event
            return event ?: throw IllegalArgumentException("Unknown action: $action")
        }

        fun translate(event: PaymentProcessingEvent): Action {
            val code = entries.find { it.event == event }?.action
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}