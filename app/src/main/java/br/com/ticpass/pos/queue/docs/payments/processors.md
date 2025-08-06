# Payment Processors

Detailed guide to payment processor implementations in the payment queue system.

## Overview

Payment processors handle the actual processing of payment transactions. The system supports multiple processor types that can handle different payment methods and scenarios.

## Processor Hierarchy

```
PaymentProcessorBase (abstract)
├── DynamicPaymentProcessor
├── AcquirerPaymentProcessor  
├── CashPaymentProcessor
└── TransactionlessProcessor
```

## PaymentProcessorBase

Abstract base class that provides common functionality for all payment processors.

```kotlin
abstract class PaymentProcessorBase : QueueProcessor<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
    protected val _events = MutableSharedFlow<ProcessingPaymentEvent>()
    override val events = _events.asSharedFlow()
    
    protected val _userInputRequests = MutableSharedFlow<UserInputRequest>()
    override val userInputRequests = _userInputRequests.asSharedFlow()
    
    protected val _userInputResponses = MutableSharedFlow<UserInputResponse>()
    
    abstract suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult
    
    override suspend fun process(item: ProcessingPaymentQueueItem): ProcessingResult {
        _events.emit(ProcessingPaymentEvent.START)
        return processPayment(item)
    }
    
    override suspend fun provideUserInput(response: UserInputResponse) {
        _userInputResponses.emit(response)
    }
    
    protected suspend fun requestUserInput(request: UserInputRequest): UserInputResponse {
        _userInputRequests.emit(request)
        
        return withTimeoutOrNull(request.timeoutMs) {
            _userInputResponses.first { it.requestId == request.id }
        } ?: UserInputResponse(request.id, null, false, true)
    }
}
```

## DynamicPaymentProcessor

Routes payments to appropriate processors based on payment method.

```kotlin
class DynamicPaymentProcessor : PaymentProcessorBase() {
    private val acquirerProcessor = AcquirerPaymentProcessor()
    private val cashProcessor = CashPaymentProcessor()
    private val transactionlessProcessor = TransactionlessProcessor()
    
    private var currentDelegateProcessor: PaymentProcessorBase? = null
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        currentDelegateProcessor = when (item.paymentMethod) {
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.DEBIT_CARD,
            PaymentMethod.PIX -> acquirerProcessor
            
            PaymentMethod.CASH -> cashProcessor
            
            PaymentMethod.VOUCHER -> transactionlessProcessor
            
            else -> throw IllegalArgumentException("Unsupported payment method: ${item.paymentMethod}")
        }
        
        // Forward events from delegate processor
        currentDelegateProcessor?.events?.collect { event ->
            _events.emit(event)
        }
        
        return currentDelegateProcessor!!.processPayment(item)
    }
    
    override suspend fun provideUserInput(response: UserInputResponse) {
        // Forward to current delegate processor
        currentDelegateProcessor?.provideUserInput(response)
        // Also emit to our own flow
        _userInputResponses.emit(response)
    }
    
    override suspend fun abort(item: ProcessingPaymentQueueItem?): Boolean {
        return currentDelegateProcessor?.abort(item) ?: false
    }
}
```

## AcquirerPaymentProcessor

Handles card-based payments through payment acquirers (Stone, PagSeguro, etc.).

```kotlin
class AcquirerPaymentProcessor : PaymentProcessorBase() {
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            // 1. Initialize transaction
            _events.emit(ProcessingPaymentEvent.CARD_REACH_OR_INSERT)
            
            // 2. Process transaction through SDK
            _events.emit(ProcessingPaymentEvent.TRANSACTION_PROCESSING)
            
            val result = when (item.paymentMethod) {
                PaymentMethod.CREDIT_CARD -> processCreditCard(item)
                PaymentMethod.DEBIT_CARD -> processDebitCard(item)
                PaymentMethod.PIX -> processPixPayment(item)
                else -> throw IllegalArgumentException("Unsupported payment method")
            }
            
            // 3. Handle transaction result
            when (result.isApproved) {
                true -> {
                    _events.emit(ProcessingPaymentEvent.APPROVAL_SUCCEEDED)
                    
                    // 4. Print receipts if needed
                    if (shouldPrintCustomerReceipt()) {
                        val printReceipt = requestUserInput(
                            UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING()
                        )
                        
                        if (printReceipt.value == true) {
                            printCustomerReceipt(result)
                        }
                    }
                    
                    _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)
                    
                    return ProcessingResult.Success(
                        atk = result.authorizationCode,
                        txId = result.transactionId
                    )
                }
                false -> {
                    _events.emit(ProcessingPaymentEvent.APPROVAL_DECLINED)
                    return ProcessingResult.Error(
                        ProcessingErrorEvent.TRANSACTION_DECLINED
                    )
                }
            }
            
        } catch (e: Exception) {
            _events.emit(ProcessingPaymentEvent.CANCELLED)
            return ProcessingResult.Error(
                mapExceptionToErrorEvent(e)
            )
        }
    }
    
    private suspend fun processCreditCard(item: ProcessingPaymentQueueItem): TransactionResult {
        // SDK-specific credit card processing
        return acquirerSdk.processCreditCard(
            amount = item.amount,
            installments = item.installments ?: 1
        )
    }
    
    private suspend fun processDebitCard(item: ProcessingPaymentQueueItem): TransactionResult {
        _events.emit(ProcessingPaymentEvent.PIN_REQUESTED)
        
        // SDK-specific debit card processing
        return acquirerSdk.processDebitCard(
            amount = item.amount
        )
    }
    
    private suspend fun processPixPayment(item: ProcessingPaymentQueueItem): TransactionResult {
        // Request PIX key from merchant if needed
        val pixKeyRequest = UserInputRequest.CONFIRM_MERCHANT_PIX_KEY()
        val pixKeyResponse = requestUserInput(pixKeyRequest)
        
        if (pixKeyResponse.isCanceled) {
            throw PaymentCancelledException("PIX key entry cancelled")
        }
        
        val pixKey = pixKeyResponse.value as? String
            ?: throw PaymentException("Invalid PIX key")
        
        // Generate PIX QR code and request scanning
        val pixCode = generatePixCode(item.amount, pixKey)
        val scanRequest = UserInputRequest.MERCHANT_PIX_SCANNING(pixCode = pixCode)
        val scanResponse = requestUserInput(scanRequest)
        
        if (scanResponse.isCanceled) {
            throw PaymentCancelledException("PIX scanning cancelled")
        }
        
        // Process PIX transaction
        return acquirerSdk.processPixPayment(
            amount = item.amount,
            pixKey = pixKey
        )
    }
    
    private suspend fun printCustomerReceipt(result: TransactionResult) {
        try {
            _events.emit(ProcessingPaymentEvent.PRINTING_RECEIPT)
            printingProvider.printCustomerReceipt(result)
        } catch (e: Exception) {
            Log.w("AcquirerProcessor", "Failed to print customer receipt", e)
            // Don't fail the entire transaction for printing issues
        }
    }
    
    override suspend fun abort(item: ProcessingPaymentQueueItem?): Boolean {
        _events.emit(ProcessingPaymentEvent.CANCELLED)
        return acquirerSdk.cancelTransaction()
    }
}
```

## CashPaymentProcessor

Handles cash payments with optional confirmation.

```kotlin
class CashPaymentProcessor : PaymentProcessorBase() {
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            _events.emit(ProcessingPaymentEvent.START)
            
            // Cash payments may require confirmation
            val confirmationRequest = UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING()
            val confirmationResponse = requestUserInput(confirmationRequest)
            
            if (confirmationResponse.isCanceled) {
                _events.emit(ProcessingPaymentEvent.CANCELLED)
                return ProcessingResult.Error(ProcessingErrorEvent.USER_CANCELED)
            }
            
            // Simulate cash processing
            delay(1000) // Simulate processing time
            
            _events.emit(ProcessingPaymentEvent.APPROVAL_SUCCEEDED)
            _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)
            
            return ProcessingResult.Success(
                atk = "CASH_${System.currentTimeMillis()}",
                txId = "CASH_TX_${item.id}"
            )
            
        } catch (e: Exception) {
            _events.emit(ProcessingPaymentEvent.CANCELLED)
            return ProcessingResult.Error(
                ProcessingErrorEvent.GENERIC_ERROR
            )
        }
    }
    
    override suspend fun abort(item: ProcessingPaymentQueueItem?): Boolean {
        _events.emit(ProcessingPaymentEvent.CANCELLED)
        return true // Cash transactions can always be cancelled
    }
}
```

## TransactionlessProcessor

Handles voucher/meal card payments that don't require traditional transaction processing.

```kotlin
class TransactionlessProcessor : PaymentProcessorBase() {
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            _events.emit(ProcessingPaymentEvent.START)
            
            // Voucher processing logic
            val voucherResult = processVoucher(item)
            
            if (voucherResult.isValid) {
                _events.emit(ProcessingPaymentEvent.APPROVAL_SUCCEEDED)
                _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)
                
                return ProcessingResult.Success(
                    atk = voucherResult.authCode,
                    txId = voucherResult.transactionId
                )
            } else {
                _events.emit(ProcessingPaymentEvent.APPROVAL_DECLINED)
                return ProcessingResult.Error(
                    ProcessingErrorEvent.VOUCHER_INVALID
                )
            }
            
        } catch (e: Exception) {
            _events.emit(ProcessingPaymentEvent.CANCELLED)
            return ProcessingResult.Error(
                mapExceptionToErrorEvent(e)
            )
        }
    }
    
    private suspend fun processVoucher(item: ProcessingPaymentQueueItem): VoucherResult {
        // Voucher-specific processing logic
        delay(500) // Simulate processing
        
        return VoucherResult(
            isValid = true,
            authCode = "VOUCHER_${System.currentTimeMillis()}",
            transactionId = "VOUCHER_TX_${item.id}"
        )
    }
    
    override suspend fun abort(item: ProcessingPaymentQueueItem?): Boolean {
        _events.emit(ProcessingPaymentEvent.CANCELLED)
        return true
    }
}
```

## Error Mapping

Payment processors map SDK-specific errors to generic error events:

```kotlin
private fun mapExceptionToErrorEvent(exception: Exception): ProcessingErrorEvent {
    return when (exception) {
        is CardReadException -> ProcessingErrorEvent.CARD_READ_ERROR
        is NetworkException -> ProcessingErrorEvent.NETWORK_ERROR
        is PaymentDeclinedException -> ProcessingErrorEvent.TRANSACTION_DECLINED
        is PaymentCancelledException -> ProcessingErrorEvent.USER_CANCELED
        is TimeoutException -> ProcessingErrorEvent.TIMEOUT
        else -> ProcessingErrorEvent.GENERIC_ERROR
    }
}
```

## Best Practices

### 1. Event Emission
Always emit appropriate events during processing:

```kotlin
override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
    _events.emit(ProcessingPaymentEvent.START)
    
    try {
        // Processing logic...
        _events.emit(ProcessingPaymentEvent.APPROVAL_SUCCEEDED)
        return ProcessingResult.Success(...)
    } catch (e: Exception) {
        _events.emit(ProcessingPaymentEvent.CANCELLED)
        return ProcessingResult.Error(...)
    } finally {
        _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE)
    }
}
```

### 2. User Input Handling
Always handle timeouts and cancellations:

```kotlin
val response = requestUserInput(request)

when {
    response.isCanceled -> {
        return ProcessingResult.Error(ProcessingErrorEvent.USER_CANCELED)
    }
    response.isTimeout -> {
        return ProcessingResult.Error(ProcessingErrorEvent.TIMEOUT)
    }
    else -> {
        // Use response.value
    }
}
```

### 3. Resource Cleanup
Ensure proper cleanup in abort scenarios:

```kotlin
override suspend fun abort(item: ProcessingPaymentQueueItem?): Boolean {
    try {
        // Cancel any ongoing operations
        acquirerSdk.cancelTransaction()
        
        // Clean up resources
        cleanup()
        
        _events.emit(ProcessingPaymentEvent.CANCELLED)
        return true
    } catch (e: Exception) {
        Log.e("PaymentProcessor", "Failed to abort transaction", e)
        return false
    }
}
```

### 4. Error Resilience
Handle SDK errors gracefully:

```kotlin
try {
    val result = acquirerSdk.processPayment(...)
    return ProcessingResult.Success(...)
} catch (e: SdkException) {
    val errorEvent = errorMapper.mapSdkError(e)
    return ProcessingResult.Error(errorEvent)
} catch (e: Exception) {
    Log.e("PaymentProcessor", "Unexpected error during payment processing", e)
    return ProcessingResult.Error(ProcessingErrorEvent.GENERIC_ERROR)
}
```
