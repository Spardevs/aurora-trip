package br.com.ticpass.pos.queue.processors.payment.models

import android.graphics.Bitmap
import br.com.ticpass.pos.queue.core.BaseProcessingEvent
import androidx.core.graphics.createBitmap

/**
 * Payment-specific events emitted during payment processing
 */
sealed class PaymentProcessingEvent : BaseProcessingEvent {
    open val transactionId: String? = null

    /**
     * Payment processing has started.
     */
    object START : PaymentProcessingEvent()

    /**
     * User should insert or reach the card.
     */
    object CARD_REACH_OR_INSERT : PaymentProcessingEvent()
    
    /**
     * Transaction has been approved.
     */
    object APPROVAL_SUCCEEDED : PaymentProcessingEvent()
    
    /**
     * Transaction has been declined.
     */
    object APPROVAL_DECLINED : PaymentProcessingEvent()
    
    /**
     * Transaction has been successfully completed.
     */
    object TRANSACTION_DONE : PaymentProcessingEvent()
    
    /**
     * Transaction is being processed.
     */
    object TRANSACTION_PROCESSING : PaymentProcessingEvent()
    
    /**
     * Transaction is going through authorization stage.
     */
    object AUTHORIZING : PaymentProcessingEvent()
    
    /**
     * Card bin information is getting verified.
     */
    object CARD_BIN_REQUESTED : PaymentProcessingEvent()
    
    /**
     * Card bin information has been checked.
     */
    object CARD_BIN_OK : PaymentProcessingEvent()
    
    /**
     * Card holder information is getting verified.
     */
    object CARD_HOLDER_REQUESTED : PaymentProcessingEvent()
    
    /**
     * Card holder information has been checked.
     */
    object CARD_HOLDER_OK : PaymentProcessingEvent()
    
    /**
     * Contactless payment has failed.
     */
    object CONTACTLESS_ERROR : PaymentProcessingEvent()
    
    /**
     * Contactless payment has been detected.
     */
    object CONTACTLESS_ON_DEVICE : PaymentProcessingEvent()
    
    /**
     * CVV information has been checked.
     */
    object CVV_OK : PaymentProcessingEvent()
    
    /**
     * CVV information is getting verified.
     */
    object CVV_REQUESTED : PaymentProcessingEvent()
    
    /**
     * Downloading tables for transaction processing.
     */
    object DOWNLOADING_TABLES : PaymentProcessingEvent()
    
    /**
     * Saving tables for transaction processing.
     */
    object SAVING_TABLES : PaymentProcessingEvent()
    
    /**
     * User should insert the card.
     */
    object USE_CHIP : PaymentProcessingEvent()
    
    /**
     * User should insert the card with a magnetic stripe.
     */
    object USE_MAGNETIC_STRIPE : PaymentProcessingEvent()
    
    /**
     * Card has been inserted.
     */
    object CARD_INSERTED : PaymentProcessingEvent()
    
    /**
     * User should remove the card.
     */
    object CARD_REMOVAL_REQUESTING : PaymentProcessingEvent()
    
    /**
     * User has removed the card.
     */
    object CARD_REMOVAL_SUCCEEDED : PaymentProcessingEvent()
    
    /**
     * Key has been inserted.
     */
    object KEY_INSERTED : PaymentProcessingEvent()
    
    /**
     * Activation has succeeded.
     */
    object ACTIVATION_SUCCEEDED : PaymentProcessingEvent()
    
    /**
     * Solving pending issues during payment processing.
     */
    object SOLVING_PENDING_ISSUES : PaymentProcessingEvent()
    
    /**
     * User should input their card PIN.
     */
    object PIN_REQUESTED : PaymentProcessingEvent()
    
    /**
     * User has input a card PIN digit.
     */
    object PIN_DIGIT_INPUT : PaymentProcessingEvent()
    
    /**
     * User has deleted a card PIN digit.
     */
    object PIN_DIGIT_REMOVED : PaymentProcessingEvent()
    
    /**
     * User card PIN has been verified.
     */
    object PIN_OK : PaymentProcessingEvent()

    /**
     * Generic success event.
     */
    object GENERIC_SUCCESS : PaymentProcessingEvent()

    /**
     * Generic error event.
     */
    object GENERIC_ERROR : PaymentProcessingEvent()
    
    /**
     * Payment process was canceled by user or system.
     */
    object CANCELLED : PaymentProcessingEvent()

    /**
     * User should swipe the card.
     */
    object SWIPE_CARD_REQUESTED : PaymentProcessingEvent()

    /**
     * User should scan the QR code (usually PIX).
     */
    data class QRCODE_SCAN(
        val qrCode: Bitmap = createBitmap(1, 1, Bitmap.Config.ALPHA_8),
        val timeoutMs: Long = 30000L,
    ) : PaymentProcessingEvent()

    /**
     * Transaction with error is being reversed.
     */
    object REVERSING_TRANSACTION_WITH_ERROR : PaymentProcessingEvent()

    /**
     * Selection of payment method is required.
     */
    object SELECT_PAYMENT_METHOD : PaymentProcessingEvent()

    /**
     * Switch payment interface.
     */
    object SWITCH_INTERFACE : PaymentProcessingEvent()

    /**
     * Payment request is in progress.
     */
    object REQUEST_IN_PROGRESS : PaymentProcessingEvent()

    /**
     * Transaction has been partially approved.
     */
    object PARTIALLY_APPROVED : PaymentProcessingEvent()

    /**
     * Transaction has been approved with updated track 3 data.
     */
    object APPROVED_UPDATE_TRACK_3 : PaymentProcessingEvent()

    /**
     * Card is approved for VIP.
     */
    object APPROVED_VIP : PaymentProcessingEvent()
}
