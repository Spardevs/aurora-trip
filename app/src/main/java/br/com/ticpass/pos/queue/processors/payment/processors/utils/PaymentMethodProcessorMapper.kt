package br.com.ticpass.pos.queue.processors.payment.processors.utils

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType

/**
 * Class responsible for mapping between payment methods and processor types
 */
object PaymentMethodProcessorMapper {
    
    /**
     * Maps payment methods to their appropriate processor types
     */
    private val methodToProcessorMap = mapOf(
        SystemPaymentMethod.CREDIT to PaymentProcessorType.ACQUIRER,
        SystemPaymentMethod.DEBIT to PaymentProcessorType.ACQUIRER,
        SystemPaymentMethod.VOUCHER to PaymentProcessorType.ACQUIRER,
        SystemPaymentMethod.PIX to PaymentProcessorType.ACQUIRER,
        SystemPaymentMethod.MERCHANT_PIX to PaymentProcessorType.MERCHANT_PIX,
        SystemPaymentMethod.CASH to PaymentProcessorType.CASH,
        SystemPaymentMethod.LN_BITCOIN to PaymentProcessorType.LN_BITCOIN
    )
    
    /**
     * Get the appropriate processor type for a payment method
     * @param method The payment method
     * @return The corresponding processor type, or ACQUIRER if not found
     */
    fun getProcessorTypeForMethod(method: SystemPaymentMethod): PaymentProcessorType {
        return methodToProcessorMap[method] ?: PaymentProcessorType.ACQUIRER
    }
}
