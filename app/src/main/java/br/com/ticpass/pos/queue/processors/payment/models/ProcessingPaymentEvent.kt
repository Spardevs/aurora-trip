package br.com.ticpass.pos.queue.processors.payment.models

import android.graphics.Bitmap
import br.com.ticpass.pos.queue.core.BaseProcessingEvent
import androidx.core.graphics.createBitmap

/**
 * Payment-specific events emitted during payment processing
 */
sealed class ProcessingPaymentEvent : BaseProcessingEvent {
    /**
     * Payment processing has started.
     */
    object START : ProcessingPaymentEvent()

    /**
     * User should insert or reach the card.
     */
    object CARD_REACH_OR_INSERT : ProcessingPaymentEvent()
    
    /**
     * Transaction has been approved.
     */
    object APPROVAL_SUCCEEDED : ProcessingPaymentEvent()
    
    /**
     * Transaction has been declined.
     */
    object APPROVAL_DECLINED : ProcessingPaymentEvent()
    
    /**
     * Transaction has been successfully completed.
     */
    object TRANSACTION_DONE : ProcessingPaymentEvent()
    
    /**
     * Transaction is being processed.
     */
    object TRANSACTION_PROCESSING : ProcessingPaymentEvent()
    
    /**
     * Transaction is going through authorization stage.
     */
    object AUTHORIZING : ProcessingPaymentEvent()
    
    /**
     * Card bin information is getting verified.
     */
    object CARD_BIN_REQUESTED : ProcessingPaymentEvent()
    
    /**
     * Card bin information has been checked.
     */
    object CARD_BIN_OK : ProcessingPaymentEvent()
    
    /**
     * Card holder information is getting verified.
     */
    object CARD_HOLDER_REQUESTED : ProcessingPaymentEvent()
    
    /**
     * Card holder information has been checked.
     */
    object CARD_HOLDER_OK : ProcessingPaymentEvent()
    
    /**
     * Contactless payment has failed.
     */
    object CONTACTLESS_ERROR : ProcessingPaymentEvent()
    
    /**
     * Contactless payment has been detected.
     */
    object CONTACTLESS_ON_DEVICE : ProcessingPaymentEvent()
    
    /**
     * CVV information has been checked.
     */
    object CVV_OK : ProcessingPaymentEvent()
    
    /**
     * CVV information is getting verified.
     */
    object CVV_REQUESTED : ProcessingPaymentEvent()
    
    /**
     * Downloading tables for transaction processing.
     */
    object DOWNLOADING_TABLES : ProcessingPaymentEvent()
    
    /**
     * Saving tables for transaction processing.
     */
    object SAVING_TABLES : ProcessingPaymentEvent()
    
    /**
     * User should insert the card.
     */
    object USE_CHIP : ProcessingPaymentEvent()
    
    /**
     * User should insert the card with a magnetic stripe.
     */
    object USE_MAGNETIC_STRIPE : ProcessingPaymentEvent()
    
    /**
     * Card has been inserted.
     */
    object CARD_INSERTED : ProcessingPaymentEvent()
    
    /**
     * User should remove the card.
     */
    object CARD_REMOVAL_REQUESTING : ProcessingPaymentEvent()
    
    /**
     * User has removed the card.
     */
    object CARD_REMOVAL_SUCCEEDED : ProcessingPaymentEvent()
    
    /**
     * Key has been inserted.
     */
    object KEY_INSERTED : ProcessingPaymentEvent()
    
    /**
     * Activation has succeeded.
     */
    object ACTIVATION_SUCCEEDED : ProcessingPaymentEvent()
    
    /**
     * Solving pending issues during payment processing.
     */
    object SOLVING_PENDING_ISSUES : ProcessingPaymentEvent()
    
    /**
     * User should input their card PIN.
     */
    object PIN_REQUESTED : ProcessingPaymentEvent()
    
    /**
     * User has input a card PIN digit.
     */
    object PIN_DIGIT_INPUT : ProcessingPaymentEvent()
    
    /**
     * User has deleted a card PIN digit.
     */
    object PIN_DIGIT_REMOVED : ProcessingPaymentEvent()
    
    /**
     * User card PIN has been verified.
     */
    object PIN_OK : ProcessingPaymentEvent()

    /**
     * Generic success event.
     */
    object GENERIC_SUCCESS : ProcessingPaymentEvent()

    /**
     * Generic error event.
     */
    object GENERIC_ERROR : ProcessingPaymentEvent()
    
    /**
     * Payment process was canceled by user or system.
     */
    object CANCELLED : ProcessingPaymentEvent()

    /**
     * User should swipe the card.
     */
    object SWIPE_CARD_REQUESTED : ProcessingPaymentEvent()

    /**
     * User should scan the QR code (usually PIX).
     */
    data class QRCODE_SCAN(
        val qrCode: Bitmap = createBitmap(1, 1, Bitmap.Config.ALPHA_8),
        val timeoutMs: Long = 30000L,
    ) : ProcessingPaymentEvent()

    /**
     * Transaction with error is being reversed.
     */
    object REVERSING_TRANSACTION_WITH_ERROR : ProcessingPaymentEvent()

    /**
     * Selection of payment method is required.
     */
    object SELECT_PAYMENT_METHOD : ProcessingPaymentEvent()

    /**
     * Switch payment interface.
     */
    object SWITCH_INTERFACE : ProcessingPaymentEvent()

    /**
     * Payment request is in progress.
     */
    object REQUEST_IN_PROGRESS : ProcessingPaymentEvent()

    /**
     * Transaction has been partially approved.
     */
    object PARTIALLY_APPROVED : ProcessingPaymentEvent()

    /**
     * Transaction has been approved with updated track 3 data.
     */
    object APPROVED_UPDATE_TRACK_3 : ProcessingPaymentEvent()

    /**
     * Card is approved for VIP.
     */
    object APPROVED_VIP : ProcessingPaymentEvent()
}
