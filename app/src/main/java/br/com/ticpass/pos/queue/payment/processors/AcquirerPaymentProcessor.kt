package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Standard Payment Processor
 * Processes payments using the acquirer SDK
 */
class AcquirerPaymentProcessor : PaymentProcessorBase() {
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // Simulate card detection
        delay(1000)
        val cardNumber = "**** **** **** ${(1000..9999).random()}"
        _events.emit(ProcessingPaymentEvent.CardDetected(item.id, "VISA", cardNumber))
        
        // Request PIN entry if amount is significant
        if (item.amount > 100) {
            delay(500)
            _events.emit(ProcessingPaymentEvent.PinRequested(item.id))
            
            // Request PIN input and wait for response
            val pinRequest = InputRequest.PinInput(
                paymentId = item.id,
                timeoutMs = 60_000L // 1 minute timeout
            )
            val pinResponse = requestInput(pinRequest)
            
            // Check if user provided PIN or canceled/timed out
            if (pinResponse == null || pinResponse.canceled) {
                _events.emit(ProcessingPaymentEvent.Failed(item.id, "PIN entry canceled or timed out"))
                return ProcessingResult.Error("PIN entry canceled or timed out")
            }
            
            // Process the PIN (in a real implementation, this would validate with the payment SDK)
            val pin = pinResponse.value as? String
            if (pin.isNullOrEmpty()) {
                _events.emit(ProcessingPaymentEvent.Failed(item.id, "Invalid PIN provided"))
                return ProcessingResult.Error("Invalid PIN provided")
            }
            
            _events.emit(ProcessingPaymentEvent.PinEntered(item.id))
        } 
        // Signature for medium transactions
        else if (item.amount > 50) {
            delay(500)
            _events.emit(ProcessingPaymentEvent.SignatureRequested(item.id))
            
            // Request signature and wait for response
            val signatureRequest = InputRequest.SignatureInput(
                paymentId = item.id,
                timeoutMs = 90_000L // 1.5 minutes timeout
            )
            val signatureResponse = requestInput(signatureRequest)
            
            // Check if user provided signature or canceled/timed out
            if (signatureResponse == null || signatureResponse.canceled) {
                _events.emit(ProcessingPaymentEvent.Failed(item.id, "Signature capture canceled or timed out"))
                return ProcessingResult.Error("Signature capture canceled or timed out")
            }
            
            // In a real implementation, this would validate and store the signature
            val signature = signatureResponse.value
            if (signature == null) {
                _events.emit(ProcessingPaymentEvent.Failed(item.id, "Invalid signature provided"))
                return ProcessingResult.Error("Invalid signature provided")
            }
        }
        
        // Final processing
        delay(1000)
        
        if (item.amount > 0) {
            // Generate transaction ID and emit completion
            val transactionId = UUID.randomUUID().toString()
            _events.emit(ProcessingPaymentEvent.Completed(item.id, item.amount, transactionId))
            return ProcessingResult.Success
        } else {
            // Emit failure for invalid amount
            _events.emit(ProcessingPaymentEvent.Failed(item.id, "Invalid amount"))
            return ProcessingResult.Error("Invalid amount")
        }
    }
}
