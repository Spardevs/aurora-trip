package br.com.ticpass.pos.queue.payment.processors

import android.util.Log
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.PaymentProcessingException
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.CancellationException
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
    private val pix = PixCodeGenerator()

    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            val pixKey = requestPixKey()
            val pixCode = generatePixCode(pixKey, item.amount)

            Log.d("MerchantPIXPaymentProcessor", "Generated PIX code: $pixCode")

            val didScan = requestPixScanning(pixCode)

            if(!didScan) return ProcessingResult.Error(
                ProcessingErrorEvent.TRANSACTION_FAILURE
            )

            val transactionId = "BTC_LN-${UUID.randomUUID().toString().substring(0, 8)}"

            _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)

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

    private suspend fun requestPixKey(): String {
        try {
            val pixKey = withContext(Dispatchers.IO) {
                requestInput(
                    InputRequest.CONFIRM_MERCHANT_PIX_KEY()
                )
            }.value as String

            if (pixKey.isBlank()) {
                throw PaymentProcessingException(
                    ProcessingErrorEvent.INVALID_PIX_KEY
                )
            }

            return pixKey
        }
        catch (exception: Exception) {
            if (exception is PaymentProcessingException) throw exception

            throw PaymentProcessingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }

    private fun generatePixCode(pixKey: String, amount: Int): String {
        try {
            val pixCode = pix.generate(
                pixKey = pixKey,
                amount = amount,
            )

            return pixCode
        }
        catch (exception: Exception) {
            if (exception is PaymentProcessingException) throw exception

            throw PaymentProcessingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }

    private suspend fun requestPixScanning(pixCode: String): Boolean {
        try {
            val didScan = withContext(Dispatchers.IO) {
                requestInput(
                    InputRequest.MERCHANT_PIX_SCANNING(pixCode = pixCode)
                )
            }.value as Boolean

            return didScan
        }
        catch (exception: Exception) {
            if (exception is PaymentProcessingException) throw exception

            throw PaymentProcessingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }
}
