package br.com.ticpass.pos.queue.processors.payment

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import stone.application.enums.TransactionStatusEnum

/**
 * Maps Stone TransactionStatusEnum to ProcessingErrorEvent
 * Provides utilities for checking transaction status errors
 */
enum class AcquirerPaymentStatusError(
    val status: TransactionStatusEnum,
    val error: ProcessingErrorEvent
) {
    DECLINED(
        TransactionStatusEnum.DECLINED,
        ProcessingErrorEvent.TRANSACTION_NOT_AUTHORIZED
    ),
    
    DECLINED_BY_CARD(
        TransactionStatusEnum.DECLINED_BY_CARD,
        ProcessingErrorEvent.OPERATION_REJECTED_BY_CARD
    ),
    
    CANCELLED(
        TransactionStatusEnum.CANCELLED,
        ProcessingErrorEvent.OPERATION_CANCELLED
    ),
    
    TECHNICAL_ERROR(
        TransactionStatusEnum.TECHNICAL_ERROR,
        ProcessingErrorEvent.TRANSACTION_FAILURE
    ),

    REJECTED(
        TransactionStatusEnum.REJECTED,
        ProcessingErrorEvent.OPERATION_NOT_AUTHORIZED
    ),
    
    WITH_ERROR(
        TransactionStatusEnum.WITH_ERROR,
        ProcessingErrorEvent.GENERIC
    ),
    
    PENDING_REVERSAL(
        TransactionStatusEnum.PENDING_REVERSAL,
        ProcessingErrorEvent.REVERSAL_PENDING
    ),
    
    PENDING(
        TransactionStatusEnum.PENDING,
        ProcessingErrorEvent.TRANSACTION_PENDING
    ),
    
    REVERSED(
        TransactionStatusEnum.REVERSED,
        ProcessingErrorEvent.CANCELLED_AWAITING_REVERSAL
    ),
    
    UNKNOWN(
        TransactionStatusEnum.UNKNOWN,
        ProcessingErrorEvent.UNEXPECTED_ERROR
    );
    
    companion object {
        /**
         * Checks if the transaction status represents an error state
         * @param status The transaction status to check
         * @return true if the status is considered an error, false otherwise
         */
        fun isError(status: TransactionStatusEnum): Boolean {
            return entries.any { it.status == status }
        }

        /**
         * Translates a TransactionStatusEnum to a ProcessingErrorEvent
         * @param status The transaction status to translate
         * @return The corresponding ProcessingErrorEvent, or UNEXPECTED_ERROR if not found
         */
        fun translate(status: TransactionStatusEnum): ProcessingErrorEvent {
            return entries.firstOrNull { it.status == status }?.error
                ?: ProcessingErrorEvent.UNEXPECTED_ERROR
        }
    }
}
