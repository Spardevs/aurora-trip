package br.com.ticpass.pos.queue.payment.processors

import android.util.Log
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Merchant PIX Payment Processor
 * Processes merchant PIX payments without using acquirer SDK
 */
class MerchantPIXPaymentProcessor : PaymentProcessorBase() {

    // Track if processor is currently being aborted
    private val isAborting = AtomicBoolean(false)

    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            val pixKey = withContext(Dispatchers.IO) {
                requestInput(
                    InputRequest.CONFIRM_MERCHANT_PIX_KEY()
                )
            }.value as String

            if (pixKey.isBlank()) {
                return ProcessingResult.Error(
                    ProcessingErrorEvent.INVALID_PIX_KEY
                )
            }

            val pix = PixCodeGenerator()
            val pixString = pix.generate(
                pixKey = pixKey,
                amount = item.amount,
            )
            Log.d("MerchantPIXPaymentProcessor", "Generated PIX String: $pixString")
            withContext(Dispatchers.IO) { delay(1500) }

            val transactionId = "BTC_LN-${UUID.randomUUID().toString().substring(0, 8)}"
            val hasLowAmount = item.amount <= 1000

            if(hasLowAmount) return ProcessingResult.Error(
                ProcessingErrorEvent.INVALID_TRANSACTION_AMOUNT
            )

            withContext(Dispatchers.IO) { delay(1000) }

            _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)

            // Use withContext to ensure we're on a background thread
            withContext(Dispatchers.IO) { delay(1500) }

            return ProcessingResult.Success(
                atk = "",
                txId = transactionId
            )
        }
        catch (e: CancellationException) {
            return ProcessingResult.Error(
                ProcessingErrorEvent.OPERATION_CANCELLED
            )
        }
        catch (e: Exception) {
            return ProcessingResult.Error(ProcessingErrorEvent.GENERIC)
        }
    }

    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        return try {
            isAborting.set(true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
