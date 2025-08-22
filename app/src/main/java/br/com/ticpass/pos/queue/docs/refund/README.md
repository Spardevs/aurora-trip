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

// Start processing (required - queue doesn't auto-start)
refundQueue.startProcessing()

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

### RefundProcessorType
Defines the types of refund processors available:

```kotlin
enum class RefundProcessorType {
    ACQUIRER,    // Uses acquirer SDK for refund processing
}
```

### DynamicRefundProcessor
Handles refund operations through processor delegation:

```kotlin
class DynamicRefundProcessor(
    private val processorMap: Map<RefundProcessorType, RefundProcessorBase>
) : RefundProcessorBase() {
    // Delegates to appropriate processor based on item's processorType
    // Forwards events and input requests from delegate processors
    // Currently supports ACQUIRER refund processing
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

## Processing Results

Refund operations return specific result types:

```kotlin
// Success result
class RefundSuccess : ProcessingResult.Success()

// Error result with specific error event
class RefundError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)
```

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

## Error Handling

Refund processing can encounter various acquirer SDK errors that are mapped to generic `ProcessingErrorEvent` objects for consistent handling. The `AcquirerRefundProcessor` handles acquirer-specific error codes and action codes through dedicated exception handling:

- **Stone Implementation**: Uses `AcquirerRefundActionCode` and `AcquirerRefundActionCodeError` for error translation
- **PagSeguro Implementation**: Uses `AcquirerRefundErrorEvent` for error code mapping
- **Common Flow**: Both implementations throw `AcquirerRefundException` with mapped `ProcessingErrorEvent`

## Refund Processor Types

The system supports the following refund processor:

- **ACQUIRER**: Multi-acquirer refund processing using ATK/TxID with flavor-specific implementations

## Advanced Features

- **Multi-Acquirer Support**: Flavor-specific implementations for different acquirer SDKs
- **ATK/TxID Based**: Uses ATK (Authorization Token Key) and Transaction ID for refund identification
- **QR Code Support**: Handles both QR code and regular transaction refunds with acquirer-specific void types
- **Dynamic Processor**: Uses `RefundProcessorRegistry` for flexible processor selection
- **Error Recovery**: Comprehensive error handling with acquirer-specific error mapping
- **Coroutine-Based**: Fully asynchronous processing with proper resource cleanup
- **Abort Handling**: Acquirer-specific abort logic with proper cleanup and cancellation
- **Action Code Translation**: Handles acquirer-specific action codes for success/error determination


## Related Documentation

- [Refund Events](events.md)
- [Generic Queue System](../README.md)
- [Error Handling](../error-handling.md)
