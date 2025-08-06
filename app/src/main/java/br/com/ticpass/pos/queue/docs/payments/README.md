# Payment Queue System

Payment-specific implementation of the generic queue management system, designed for processing payment transactions with various payment methods and processors.

## Quick Start

```kotlin
// Create a payment queue
val paymentQueue = ProcessingPaymentQueueFactory().createDynamicPaymentQueue(
    storage = ProcessingPaymentStorage(dao),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.IMMEDIATE,
    scope = viewModelScope
)

// Enqueue a payment
val paymentItem = ProcessingPaymentQueueItem(
    id = UUID.randomUUID().toString(),
    amount = 100.0,
    commission = 5.0,
    paymentMethod = PaymentMethod.CREDIT_CARD,
    status = QueueItemStatus.PENDING
)
paymentQueue.enqueue(paymentItem)

// Observe payment events
paymentQueue.processorEvents.collect { event ->
    when (event) {
        ProcessingPaymentEvent.START -> { /* Payment started */ }
        ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> { /* Show card prompt */ }
        ProcessingPaymentEvent.TRANSACTION_PROCESSING -> { /* Show processing */ }
        ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> { /* Payment approved */ }
        ProcessingPaymentEvent.APPROVAL_DECLINED -> { /* Payment declined */ }
        // Handle other events...
    }
}
```

## Payment Queue Components

### ProcessingPaymentQueueItem
Payment-specific queue item implementation:

```kotlin
data class ProcessingPaymentQueueItem(
    override val id: String,
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val amount: Double,
    val commission: Double,
    val paymentMethod: PaymentMethod,
    // ... other payment fields
) : QueueItem
```

### Payment Processors

#### DynamicPaymentProcessor
Routes payments to appropriate processors based on payment method:

```kotlin
class DynamicPaymentProcessor : PaymentProcessorBase() {
    // Automatically delegates to:
    // - AcquirerPaymentProcessor for card payments
    // - CashPaymentProcessor for cash payments
    // - TransactionlessProcessor for transactionless payments
}
```

#### AcquirerPaymentProcessor
Handles card-based payments through payment acquirers:

```kotlin
class AcquirerPaymentProcessor : PaymentProcessorBase() {
    // Processes credit/debit card transactions
    // Emits payment events during processing
    // Handles receipt printing
}
```

#### CashPaymentProcessor
Handles cash payments:

```kotlin
class CashPaymentProcessor : PaymentProcessorBase() {
    // Processes cash transactions
    // May require user confirmation
}
```

### Payment Events

Payment processors emit specific events during processing:

```kotlin
sealed class ProcessingPaymentEvent : BaseProcessingEvent {
    object START : ProcessingPaymentEvent()
    object CARD_REACH_OR_INSERT : ProcessingPaymentEvent()
    object TRANSACTION_PROCESSING : ProcessingPaymentEvent()
    object APPROVAL_SUCCEEDED : ProcessingPaymentEvent()
    object APPROVAL_DECLINED : ProcessingPaymentEvent()
    object TRANSACTION_DONE : ProcessingPaymentEvent()
    object PIN_REQUESTED : ProcessingPaymentEvent()
    object CANCELLED : ProcessingPaymentEvent()
    // ... other events
}
```

### User Input Requests

Payment processors may request user input:

```kotlin
sealed class UserInputRequest {
    data class CONFIRM_CUSTOMER_RECEIPT_PRINTING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 10_000L
    ) : UserInputRequest()
    
    data class CONFIRM_MERCHANT_PIX_KEY(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 60_000L
    ) : UserInputRequest()
    
    data class MERCHANT_PIX_SCANNING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 60_000L,
        val pixCode: String
    ) : UserInputRequest()
}
```

## Usage Examples

### Basic Payment Processing

```kotlin
class PaymentViewModel : ViewModel() {
    private val paymentQueue = ProcessingPaymentQueueFactory().createDynamicPaymentQueue(...)
    
    fun processPayment(amount: Double, paymentMethod: PaymentMethod) {
        val paymentItem = ProcessingPaymentQueueItem(
            id = UUID.randomUUID().toString(),
            amount = amount,
            commission = calculateCommission(amount),
            paymentMethod = paymentMethod
        )
        
        viewModelScope.launch {
            paymentQueue.enqueue(paymentItem)
        }
    }
    
    fun observePaymentEvents() {
        viewModelScope.launch {
            paymentQueue.processorEvents.collect { event ->
                when (event) {
                    ProcessingPaymentEvent.START -> {
                        updateUI("Payment started")
                    }
                    ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> {
                        updateUI("Please insert or tap your card")
                        showCardAnimation()
                    }
                    ProcessingPaymentEvent.TRANSACTION_PROCESSING -> {
                        updateUI("Processing transaction...")
                        showProcessingAnimation()
                    }
                    ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> {
                        updateUI("Payment approved!")
                        showSuccessAnimation()
                    }
                    ProcessingPaymentEvent.APPROVAL_DECLINED -> {
                        updateUI("Payment declined")
                        showErrorAnimation()
                    }
                    ProcessingPaymentEvent.TRANSACTION_DONE -> {
                        updateUI("Transaction completed")
                    }
                }
            }
        }
    }
}
```

### Handling User Input Requests

```kotlin
fun observeUserInputRequests() {
    viewModelScope.launch {
        paymentQueue.processor.userInputRequests.collect { request ->
            when (request) {
                is UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
                    showReceiptConfirmationDialog(
                        onConfirm = {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, true)
                            )
                        },
                        onCancel = {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, false)
                            )
                        }
                    )
                }
                is UserInputRequest.CONFIRM_MERCHANT_PIX_KEY -> {
                    showPixKeyDialog(
                        onConfirm = { pixKey ->
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, pixKey)
                            )
                        },
                        onCancel = {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, null, true)
                            )
                        }
                    )
                }
                is UserInputRequest.MERCHANT_PIX_SCANNING -> {
                    showPixScanningDialog(
                        pixCode = request.pixCode,
                        onScanned = { result ->
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, result)
                            )
                        },
                        onCancel = {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, null, true)
                            )
                        }
                    )
                }
            }
        }
    }
}
```

### Error Handling

```kotlin
fun observeQueueInputRequests() {
    viewModelScope.launch {
        paymentQueue.queueInputRequests.collect { request ->
            when (request) {
                is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                    showErrorHandlingDialog(
                        error = request.error,
                        onRetry = {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorRetry(request.id)
                            )
                        },
                        onSkip = {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorSkip(request.id)
                            )
                        },
                        onAbort = {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorAbort(request.id)
                            )
                        },
                        onAbortAll = {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorAbortAll(request.id)
                            )
                        }
                    )
                }
                is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                    showConfirmationDialog(
                        message = "Process payment ${request.currentItemIndex + 1} of ${request.totalItems}?",
                        onConfirm = {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.proceed(request.id)
                            )
                        },
                        onSkip = {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.skip(request.id)
                            )
                        }
                    )
                }
            }
        }
    }
}
```

## Payment Methods

The system supports various payment methods:

- **CREDIT_CARD**: Credit card transactions
- **DEBIT_CARD**: Debit card transactions  
- **PIX**: Brazilian instant payment system
- **CASH**: Cash transactions
- **VOUCHER**: Voucher/meal card transactions

## Error Handling

Payment processing can encounter various errors that are handled through the generic error handling system:

- **Card Errors**: Card read failures, blocked cards, etc.
- **Network Errors**: Connection issues with payment processors
- **Transaction Errors**: Declined transactions, insufficient funds, etc.
- **System Errors**: Hardware failures, configuration issues, etc.

All errors are mapped to generic `ProcessingErrorEvent` objects for consistent handling across different payment processors.

## Advanced Features

- **Receipt Printing**: Automatic customer and merchant receipt printing
- **Transaction Logging**: Comprehensive transaction audit trail
- **Multi-Processor Support**: Handle different payment types in a single queue
- **Retry Logic**: Automatic retry for transient failures
- **Timeout Handling**: Configurable timeouts for user interactions

## Related Documentation

- [Generic Queue System](../README.md)
- [Payment Processors](processors.md)
- [Payment Events](events.md)
- [Usage Examples](examples.md)
