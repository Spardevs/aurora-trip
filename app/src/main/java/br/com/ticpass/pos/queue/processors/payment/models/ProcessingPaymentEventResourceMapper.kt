package br.com.ticpass.pos.queue.processors.payment.models

import br.com.ticpass.pos.R

/**
 * Maps ProcessingPaymentEvent types to string resource keys
 * This centralized mapping makes it easy to maintain error message mappings
 */
object ProcessingPaymentEventResourceMapper {
    
    /**
     * Get the string resource key for a ProcessingPaymentEvent
     * @param event The ProcessingPaymentEvent to map
     * @return The string resource key corresponding to the event
     */
    fun getErrorResourceKey(event: ProcessingPaymentEvent): Int {
        return when (event) {
            is ProcessingPaymentEvent.APPROVED_VIP -> R.string.error_approved_vip
            is ProcessingPaymentEvent.START -> R.string.event_start
            is ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> R.string.event_card_reach_or_insert
            is ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> R.string.event_approval_succeeded
            is ProcessingPaymentEvent.APPROVAL_DECLINED -> R.string.event_approval_declined
            is ProcessingPaymentEvent.TRANSACTION_DONE -> R.string.event_transaction_done
            is ProcessingPaymentEvent.TRANSACTION_PROCESSING -> R.string.event_transaction_processing
            is ProcessingPaymentEvent.AUTHORIZING -> R.string.event_authorizing
            is ProcessingPaymentEvent.CARD_BIN_REQUESTED -> R.string.event_card_bin_requested
            is ProcessingPaymentEvent.CARD_BIN_OK -> R.string.event_card_bin_ok
            is ProcessingPaymentEvent.CARD_HOLDER_REQUESTED -> R.string.event_card_holder_requested
            is ProcessingPaymentEvent.CARD_HOLDER_OK -> R.string.event_card_holder_ok
            is ProcessingPaymentEvent.CONTACTLESS_ERROR -> R.string.event_contactless_error
            is ProcessingPaymentEvent.CONTACTLESS_ON_DEVICE -> R.string.event_contactless_on_device
            is ProcessingPaymentEvent.CVV_OK -> R.string.event_cvv_ok
            is ProcessingPaymentEvent.CVV_REQUESTED -> R.string.event_cvv_requested
            is ProcessingPaymentEvent.DOWNLOADING_TABLES -> R.string.event_downloading_tables
            is ProcessingPaymentEvent.SAVING_TABLES -> R.string.event_saving_tables
            is ProcessingPaymentEvent.USE_CHIP -> R.string.event_use_chip
            is ProcessingPaymentEvent.USE_MAGNETIC_STRIPE -> R.string.event_use_magnetic_stripe
            is ProcessingPaymentEvent.CARD_REMOVAL_REQUESTING -> R.string.event_card_removal_requesting
            is ProcessingPaymentEvent.KEY_INSERTED -> R.string.event_key_inserted
            is ProcessingPaymentEvent.ACTIVATION_SUCCEEDED -> R.string.event_activation_succeeded
            is ProcessingPaymentEvent.SOLVING_PENDING_ISSUES -> R.string.event_solving_pending_issues
            is ProcessingPaymentEvent.PIN_REQUESTED -> R.string.event_pin_requested
            is ProcessingPaymentEvent.CARD_INSERTED -> R.string.event_card_inserted
            is ProcessingPaymentEvent.PIN_DIGIT_INPUT -> R.string.event_pin_digit_input
            is ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> R.string.event_pin_digit_removed
            is ProcessingPaymentEvent.CARD_REMOVAL_SUCCEEDED -> R.string.event_card_removal_succeeded
            is ProcessingPaymentEvent.PIN_OK -> R.string.event_pin_ok
            is ProcessingPaymentEvent.GENERIC_SUCCESS -> R.string.event_generic_success
            is ProcessingPaymentEvent.GENERIC_ERROR -> R.string.event_generic_error
            is ProcessingPaymentEvent.CANCELLED -> R.string.event_cancelled
            is ProcessingPaymentEvent.QRCODE_SCAN -> R.string.event_qrcode_scan
            is ProcessingPaymentEvent.REVERSING_TRANSACTION_WITH_ERROR -> R.string.reversing_transaction_with_error
            is ProcessingPaymentEvent.SELECT_PAYMENT_METHOD -> R.string.select_payment_method
            is ProcessingPaymentEvent.SWIPE_CARD_REQUESTED -> R.string.swipe_card_requested
            is ProcessingPaymentEvent.SWITCH_INTERFACE -> R.string.switch_interface
            is ProcessingPaymentEvent.REQUEST_IN_PROGRESS -> R.string.request_in_progress
            is ProcessingPaymentEvent.PARTIALLY_APPROVED -> R.string.partially_approved
            is ProcessingPaymentEvent.APPROVED_UPDATE_TRACK_3 -> R.string.approved_update_track_3
        }
    }
}
