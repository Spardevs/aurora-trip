package br.com.ticpass.pos.queue.processors.refund

import br.com.ticpass.pos.queue.processors.refund.models.RefundEvent


enum class AcquirerRefundActionCode(val actionCode: String, val event: RefundEvent) {
    APPROVED(
        "0000",
        RefundEvent.SUCCESS
    ),

    REQUEST_IN_PROGRESS(
        "2824",
        RefundEvent.PROCESSING
    );

    companion object {
        fun translate(actionCode: String): RefundEvent {
            val event = entries.find { it.actionCode == actionCode }?.event
            return event ?: throw IllegalArgumentException("Unknown action: $actionCode")
        }

        fun translate(event: RefundEvent): String {
            val code = entries.find { it.event == event }?.actionCode
            return code ?: throw IllegalArgumentException("Unknown event: $event")
        }
    }
}