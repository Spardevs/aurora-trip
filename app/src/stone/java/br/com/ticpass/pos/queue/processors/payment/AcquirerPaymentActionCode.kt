package br.com.ticpass.pos.queue.processors.payment

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent

enum class AcquirerPaymentActionCode(val actionCode: String, val event: ProcessingPaymentEvent) {
    TRANSACTION_DONE(
        "9999999",
        ProcessingPaymentEvent.TRANSACTION_DONE
    ),

    APPROVED(
        "0000",
        ProcessingPaymentEvent.APPROVAL_SUCCEEDED
    ),

    REQUEST_IN_PROGRESS(
        "2824",
        ProcessingPaymentEvent.REQUEST_IN_PROGRESS
    ),

    PARTIALLY_APPROVED(
        "0002",
        ProcessingPaymentEvent.PARTIALLY_APPROVED
    ),

    APPROVED_VIP(
        "0003",
        ProcessingPaymentEvent.APPROVED_VIP
    ),

    APPROVED_UPDATE_TRACK_3(
        "0004",
        ProcessingPaymentEvent.APPROVED_UPDATE_TRACK_3
    );

    companion object {
        fun translate(actionCode: String): ProcessingPaymentEvent {
            val event = entries.find { it.actionCode == actionCode }?.event
            return event ?: throw IllegalArgumentException("Unknown action: $actionCode")
        }

        fun translate(event: ProcessingPaymentEvent): String {
            val code = entries.find { it.event == event }?.actionCode
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}