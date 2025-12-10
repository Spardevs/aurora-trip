package br.com.ticpass.pos.core.queue.processors.payment

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEvent

enum class AcquirerPaymentActionCode(val actionCode: String, val event: PaymentProcessingEvent) {
    TRANSACTION_DONE(
        "9999999",
        PaymentProcessingEvent.TRANSACTION_DONE
    ),

    APPROVED(
        "0000",
        PaymentProcessingEvent.APPROVAL_SUCCEEDED
    ),

    REQUEST_IN_PROGRESS(
        "2824",
        PaymentProcessingEvent.REQUEST_IN_PROGRESS
    ),

    PARTIALLY_APPROVED(
        "0002",
        PaymentProcessingEvent.PARTIALLY_APPROVED
    ),

    APPROVED_VIP(
        "0003",
        PaymentProcessingEvent.APPROVED_VIP
    ),

    APPROVED_UPDATE_TRACK_3(
        "0004",
        PaymentProcessingEvent.APPROVED_UPDATE_TRACK_3
    );

    companion object {
        fun translate(actionCode: String): PaymentProcessingEvent {
            val event = entries.find { it.actionCode == actionCode }?.event
            return event ?: throw IllegalArgumentException("Unknown action: $actionCode")
        }

        fun translate(event: PaymentProcessingEvent): String {
            val code = entries.find { it.event == event }?.actionCode
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}