# Refund Queue System

Refund-specific implementation of the generic queue management system for handling transaction reversals and refund processing.

**Supported Operations**: Multi-acquirer refund processing with ATK/TxID-based transaction reversals.

## Quick Start

```kotlin
// Create a refund queue
val refundQueue = RefundQueueFactory().createDynamicRefundQueue(
    storage = RefundStorage(dao),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.IMMEDIATE,
    scope = viewModelScope
)

// Enqueue a refund
val refundItem = RefundQueueItem(
    id = UUID.randomUUID().toString(),
    processorType = RefundProcessorType.ACQUIRER,
    atk = "transaction_atk_123",
    txId = "transaction_id_456",
    isQRCode = false,
    status = QueueItemStatus.PENDING
)
refundQueue.enqueue(refundItem)

// Start processing (required - queue won't start automatically)
refundQueue.startProcessing()

// Observe refund events
refundQueue.events.collect { event ->
    when (event) {
        RefundEvent.START -> { /* Refund started */ }
        RefundEvent.PROCESSING -> { /* Processing refund */ }
        RefundEvent.REFUNDING -> { /* Refund in progress */ }
        RefundEvent.SUCCESS -> { /* Refund completed successfully */ }
        RefundEvent.CANCELLED -> { /* Refund cancelled */ }
    }
}
```

## Refund Queue Components

### RefundQueueItem
Refund-specific queue item implementation:

```kotlin
data class RefundQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val processorType: RefundProcessorType,
    val atk: String,
    val txId: String,
    val isQRCode: Boolean = false,
) : QueueItem
```

### AcquirerRefundProcessor
Handles refund processing through acquirer SDKs with flavor-specific implementations:

```kotlin
class AcquirerRefundProcessor : RefundProcessorBase() {
    // Processes refund transactions via acquirer SDKs
    // Uses ATK and TxID for transaction identification
    // Emits refund events during processing
    // Handles acquirer-specific refund callbacks and error handling
    // Supports QR code and regular transaction refunds
    // Multi-acquirer support with flavor-specific implementations
}
```

### Refund Events

Refund processors emit specific events during processing:

```kotlin
sealed class RefundEvent : BaseProcessingEvent {
    /**
     * Refund processing has started.
     */
    object START : RefundEvent()

    /**
     * Refund process was canceled by user or system.
     */
    object CANCELLED : RefundEvent()

    /**
     * Refund is being processed.
     */
    object PROCESSING : RefundEvent()

    /**
     * Refund is currently in progress.
     */
    object REFUNDING : RefundEvent()

    /**
     * Refund is success.
     */
    object SUCCESS : RefundEvent()
}
```

### User Input Requests

Refund processors may request user input for printer network configuration:

```kotlin
sealed class UserInputRequest {
    /**
     * Request to confirm printer network information like IP address, port, etc.
     */
    data class CONFIRM_PRINTER_NETWORK_INFO(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 5_000L, // 5 seconds default timeout
    ) : UserInputRequest()
}
```

## Implementation Details

### Acquirer-Specific Implementations

The `AcquirerRefundProcessor` has flavor-specific implementations that handle different acquirer SDKs:

#### Common Processing Flow
1. **Initialize**: Set up refund data using ATK and TxID
2. **Emit Events**: `PROCESSING` → `REFUNDING` → `SUCCESS`/`CANCELLED`
3. **Execute**: Call acquirer-specific refund methods
4. **Handle Results**: Process success/error responses
5. **Cleanup**: Release resources and reset coroutine scopes

#### Acquirer-Specific Details

**Stone Implementation:**
- Uses `CancellationProvider` from Stone SDK
- Handles `TransactionObject` for action code processing
- Implements `StoneCallbackInterface` for async callbacks
- Translates Stone `ErrorsEnum` to generic error events

**PagSeguro Implementation:**
- Uses `PlugPagVoidData` with transaction details
- Supports `VOID_PAYMENT` and `VOID_QRCODE` void types
- Direct synchronous `voidPayment()` call
- Handles `PlugPagException` for error scenarios

## Usage Examples

### Basic Refund Processing

```kotlin
class RefundViewModel : ViewModel() {
    private val refundQueue = RefundQueueFactory().createDynamicRefundQueue(...)
    
    fun enqueueRefund(
        atk: String,
        txId: String,
        isQRCode: Boolean,
        processorType: RefundProcessorType
    ) {
        val refundItem = RefundQueueItem(
            processorType = processorType,
            atk = atk,
            txId = txId,
            isQRCode = isQRCode
        )
        
        viewModelScope.launch {
            refundQueue.enqueue(refundItem)
        }
    }
    
    fun observeRefundEvents() {
        viewModelScope.launch {
            refundQueue.events.collect { event ->
                when (event) {
                    RefundEvent.START -> {
                        updateUI("Refund started")
                    }
                    RefundEvent.PROCESSING -> {
                        updateUI("Processing refund...")
                        showProcessingAnimation()
                    }
                    RefundEvent.REFUNDING -> {
                        updateUI("Refund in progress...")
                        showProcessingAnimation()
                    }
                    RefundEvent.SUCCESS -> {
                        updateUI("Refund completed successfully")
                        showSuccessAnimation()
                    }
                    RefundEvent.CANCELLED -> {
                        updateUI("Refund cancelled")
                        showErrorAnimation()
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
        refundQueue.processor.userInputRequests.collect { request ->
            when (request) {
                is UserInputRequest.CONFIRM_PRINTER_NETWORK_INFO -> {
                    showPrinterNetworkDialog(
                        onConfirm = { networkInfo ->
                            refundQueue.processor.provideUserInput(
                                UserInputResponse(request.id, networkInfo)
                            )
                        },
                        onCancel = {
                            refundQueue.processor.provideUserInput(
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
        refundQueue.queueInputRequests.collect { request ->
            when (request) {
                is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                    showErrorHandlingDialog(
                        error = request.error,
                        onRetry = {
                            refundQueue.provideQueueInput(
                                QueueInputResponse.onErrorRetry(request.id)
                            )
                        },
                        onSkip = {
                            refundQueue.provideQueueInput(
                                QueueInputResponse.onErrorSkip(request.id)
                            )
                        },
                        onAbort = {
                            refundQueue.provideQueueInput(
                                QueueInputResponse.onErrorAbort(request.id)
                            )
                        }
                    )
                }
                is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
                    showConfirmationDialog(
                        message = "Process refund ${request.currentItemIndex + 1} of ${request.totalItems}?",
                        onConfirm = {
                            refundQueue.provideQueueInput(
                                QueueInputResponse.proceed(request.id)
                            )
                        },
                        onSkip = {
                            refundQueue.provideQueueInput(
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

## Refund Processor Types

The system supports the following refund processor:

- **ACQUIRER**: Stone acquirer-based refund processing using ATK/TxID

## Processing Results

Refund processing returns specific result types:

```kotlin
class RefundSuccess : ProcessingResult.Success()
class RefundError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)
```

## Error Handling

Refund processing can encounter various acquirer SDK errors that are mapped to generic `ProcessingErrorEvent` objects for consistent handling. The `AcquirerRefundProcessor` handles acquirer-specific error codes and action codes through dedicated exception handling:

- **Stone Implementation**: Uses `AcquirerRefundActionCode` and `AcquirerRefundActionCodeError` for error translation
- **PagSeguro Implementation**: Uses `AcquirerRefundErrorEvent` for error code mapping
- **Common Flow**: Both implementations throw `AcquirerRefundException` with mapped `ProcessingErrorEvent`

## Advanced Features

- **Multi-Acquirer Support**: Flavor-specific implementations for different acquirer SDKs
- **ATK/TxID Based**: Uses ATK (Authorization Token Key) and Transaction ID for refund identification
- **QR Code Support**: Handles both QR code and regular transaction refunds with acquirer-specific void types
- **Dynamic Processor**: Uses `RefundProcessorRegistry` for flexible processor selection
- **Interactive Processing**: Built-in user input handling for printer network configuration
- **Error Recovery**: Comprehensive retry/skip/abort error handling with acquirer-specific error mapping
- **Coroutine-Based**: Fully asynchronous processing with proper resource cleanup
- **Abort Handling**: Acquirer-specific abort logic with proper cleanup and cancellation
- **Receipt Integration**: Automatic receipt printing support where available
- **Action Code Translation**: Handles acquirer-specific action codes for success/error determination

## Related Documentation

- [Generic Queue System](../README.md)
- [Payment Queue System](../payments/README.md)
- [Print Queue System](../printing/README.md)
- [Refund Examples](examples.md)
