package br.com.ticpass.pos.queue.processors.payment.processors.impl

import br.com.ticpass.pos.payment.utils.PixCodeGenerator
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.PaymentError
import br.com.ticpass.pos.queue.models.PaymentSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.payment.exceptions.PaymentProcessingException
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import br.com.ticpass.pos.queue.processors.payment.processors.core.PaymentProcessorBase
import javax.inject.Inject

/**
 * Merchant PIX Payment Processor
 * Processes merchant PIX payments without using acquirer SDK.
 */
class MerchantPIXPaymentProcessor @Inject constructor() : PaymentProcessorBase() {

    // Track if processor is currently being aborted
    private val isAborting = AtomicBoolean(false)
    private val pix = PixCodeGenerator()

    override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
        try {
            _events.emit(PaymentProcessingEvent.TRANSACTION_PROCESSING)

            val transactionId = "BTC_LN-${UUID.randomUUID().toString().substring(0, 8)}"
            val pixKey = requestPixKey()
            val pixCode = pix.generate(
                pixKey = pixKey,
                amount = item.amount,
            )

            val didScan = requestPixScanning(pixCode)

            if(!didScan) return PaymentError(
                ProcessingErrorEvent.TRANSACTION_FAILURE
            )

            _events.emit(PaymentProcessingEvent.AUTHORIZING)

            withContext(Dispatchers.IO) { delay(1500) }

            _events.emit(PaymentProcessingEvent.TRANSACTION_DONE)

            withContext(Dispatchers.IO) { delay(300) }

            return PaymentSuccess(
                atk = "",
                txId = transactionId
            )
        }
        catch (e: PaymentProcessingException) {
            return PaymentError(e.error)
        }
        catch (e: Exception) {
            return PaymentError(ProcessingErrorEvent.GENERIC)
        }
    }

    override suspend fun onAbort(item: PaymentProcessingQueueItem?): Boolean {
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
                requestUserInput(
                    UserInputRequest.CONFIRM_MERCHANT_PIX_KEY()
                )
            }.value as? String ?: ""

            if (pixKey.isBlank()) {
                throw PaymentProcessingException(
                    ProcessingErrorEvent.INVALID_PIX_KEY
                )
            }

            return pixKey
        }
        catch (exception: PaymentProcessingException) {
            throw exception
        }
        catch (exception: Exception) {
            throw PaymentProcessingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }

    private suspend fun requestPixScanning(pixCode: String): Boolean {
        try {
            val didScan = withContext(Dispatchers.IO) {
                requestUserInput(
                    UserInputRequest.MERCHANT_PIX_SCANNING(pixCode = pixCode)
                )
            }.value as? Boolean ?: true

            return didScan
        }
        catch (exception: PaymentProcessingException) {
            throw exception
        }
        catch (exception: Exception) {
            throw PaymentProcessingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }
}
