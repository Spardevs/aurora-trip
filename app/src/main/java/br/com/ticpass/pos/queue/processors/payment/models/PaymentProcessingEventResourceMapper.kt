package br.com.ticpass.pos.queue.processors.payment.models

import br.com.ticpass.pos.R

/**
 * Maps PaymentProcessingEvent types to string resource keys
 * This centralized mapping makes it easy to maintain error message mappings
 */
object PaymentProcessingEventResourceMapper {

    /**
     * Get the string resource key for a PaymentProcessingEvent
     * @param event The PaymentProcessingEvent to map
     * @return The string resource key corresponding to the event
     */
    fun getErrorResourceKey(event: PaymentProcessingEvent): Int {
        return when (event) {
            is PaymentProcessingEvent.APPROVED_VIP -> R.string.error_approved_vip
            is PaymentProcessingEvent.START -> R.string.event_start
            is PaymentProcessingEvent.CARD_REACH_OR_INSERT -> R.string.event_card_reach_or_insert
            is PaymentProcessingEvent.APPROVAL_SUCCEEDED -> R.string.event_approval_succeeded
            is PaymentProcessingEvent.APPROVAL_DECLINED -> R.string.event_approval_declined
            is PaymentProcessingEvent.TRANSACTION_DONE -> R.string.event_transaction_done
            is PaymentProcessingEvent.TRANSACTION_PROCESSING -> R.string.event_transaction_processing
            is PaymentProcessingEvent.AUTHORIZING -> R.string.event_authorizing
            is PaymentProcessingEvent.CARD_BIN_REQUESTED -> R.string.event_card_bin_requested
            is PaymentProcessingEvent.CARD_BIN_OK -> R.string.event_card_bin_ok
            is PaymentProcessingEvent.CARD_HOLDER_REQUESTED -> R.string.event_card_holder_requested
            is PaymentProcessingEvent.CARD_HOLDER_OK -> R.string.event_card_holder_ok
            is PaymentProcessingEvent.CONTACTLESS_ERROR -> R.string.event_contactless_error
            is PaymentProcessingEvent.CONTACTLESS_ON_DEVICE -> R.string.event_contactless_on_device
            is PaymentProcessingEvent.CVV_OK -> R.string.event_cvv_ok
            is PaymentProcessingEvent.CVV_REQUESTED -> R.string.event_cvv_requested
            is PaymentProcessingEvent.DOWNLOADING_TABLES -> R.string.event_downloading_tables
            is PaymentProcessingEvent.SAVING_TABLES -> R.string.event_saving_tables
            is PaymentProcessingEvent.USE_CHIP -> R.string.event_use_chip
            is PaymentProcessingEvent.USE_MAGNETIC_STRIPE -> R.string.event_use_magnetic_stripe
            is PaymentProcessingEvent.CARD_REMOVAL_REQUESTING -> R.string.event_card_removal_requesting
            is PaymentProcessingEvent.KEY_INSERTED -> R.string.event_key_inserted
            is PaymentProcessingEvent.ACTIVATION_SUCCEEDED -> R.string.event_activation_succeeded
            is PaymentProcessingEvent.SOLVING_PENDING_ISSUES -> R.string.event_solving_pending_issues
            is PaymentProcessingEvent.PIN_REQUESTED -> R.string.event_pin_requested
            is PaymentProcessingEvent.CARD_INSERTED -> R.string.event_card_inserted
            is PaymentProcessingEvent.PIN_DIGIT_INPUT -> R.string.event_pin_digit_input
            is PaymentProcessingEvent.PIN_DIGIT_REMOVED -> R.string.event_pin_digit_removed
            is PaymentProcessingEvent.CARD_REMOVAL_SUCCEEDED -> R.string.event_card_removal_succeeded
            is PaymentProcessingEvent.PIN_OK -> R.string.event_pin_ok
            is PaymentProcessingEvent.GENERIC_SUCCESS -> R.string.event_generic_success
            is PaymentProcessingEvent.GENERIC_ERROR -> R.string.event_generic_error
            is PaymentProcessingEvent.CANCELLED -> R.string.event_cancelled
            is PaymentProcessingEvent.QRCODE_SCAN -> R.string.event_qrcode_scan
            is PaymentProcessingEvent.REVERSING_TRANSACTION_WITH_ERROR -> R.string.reversing_transaction_with_error
            is PaymentProcessingEvent.SELECT_PAYMENT_METHOD -> R.string.select_payment_method
            is PaymentProcessingEvent.SWIPE_CARD_REQUESTED -> R.string.swipe_card_requested
            is PaymentProcessingEvent.SWITCH_INTERFACE -> R.string.switch_interface
            is PaymentProcessingEvent.REQUEST_IN_PROGRESS -> R.string.request_in_progress
            is PaymentProcessingEvent.PARTIALLY_APPROVED -> R.string.partially_approved
            is PaymentProcessingEvent.APPROVED_UPDATE_TRACK_3 -> R.string.approved_update_track_3
            // Add the missing case for PIX_QRCODE_GENERATED
            is PaymentProcessingEvent.PIX_QRCODE_GENERATED -> R.string.event_pix_qrcode_generated
        }
    }
}