# HybridQueueManager Deep Dive

Comprehensive guide to the `HybridQueueManager` class, the core component of the queue management system.

## Overview

The `HybridQueueManager` is a generic, processor-agnostic queue manager that combines in-memory operations with optional persistence strategies. It orchestrates the entire queue processing lifecycle.

## Class Definition

```kotlin
class HybridQueueManager<T : QueueItem, E : BaseProcessingEvent>(
    private val storage: QueueStorage<T>,
    internal val processor: QueueProcessor<T, E>,
    private val persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
    val startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
)
```

## Key Features

### Processor-Agnostic Design
The queue manager works with any queue item type and processor implementation:

```kotlin
// Payment queue
val paymentQueue = HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>(...)

// Print queue  
val printQueue = HybridQueueManager<PrintQueueItem, PrintingEvent>(...)

// Custom queue
val customQueue = HybridQueueManager<MyQueueItem, MyEvent>(...)
```

### Generic Item Management
Provides helper methods for UseCase layer modifications:

```kotlin
// Get current item being processed
val currentItem = queueManager.getCurrentItem()

// Replace current item with modified version
queueManager.replaceCurrentItem(modifiedItem)
```

### Queue-Level Input Requests
Supports generic confirmation and error handling requests:

```kotlin
// Confirmation before processing next item
QueueInputRequest.CONFIRM_NEXT_PROCESSOR(
    currentItemIndex = 0,
    totalItems = 3,
    currentItemId = "item1",
    nextItemId = "item2"
)

// Error handling request
QueueInputRequest.ERROR_RETRY_OR_SKIP(
    itemId = "item1",
    error = ProcessingErrorEvent.NETWORK_ERROR
)
```

## Processing Lifecycle

### 1. Item Enqueueing
```kotlin
suspend fun enqueue(item: T) {
    // Add to in-memory queue
    // Persist if strategy requires it
    // Start processing if startMode is IMMEDIATE
}
```

### 2. Processing Loop
```kotlin
// For each item in queue:
// 1. Update status to PROCESSING
// 2. Call processor.process(item)
// 3. Handle result (Success/Error)
// 4. Update item status
// 5. Emit processing state
```

### 3. Error Handling
When processing fails:
1. Emit `ERROR_RETRY_OR_SKIP` request
2. Wait for user response
3. Execute action based on response:
   - **RETRY**: Process same item again
   - **SKIP**: Move item to end of queue
   - **ABORT**: Remove item from queue
   - **ABORT_ALL**: Cancel entire queue

### 4. Skip Logic
Items are moved to end of queue rather than removed:
```kotlin
// Skip moves item to end for later processing
private suspend fun skipCurrentItem() {
    val item = currentItem
    removeFromCurrent()
    addToEnd(item)
}
```

## State Management

### Processing States
The queue manager emits various processing states:

```kotlin
sealed class ProcessingState<T : QueueItem> {
    class QueueIdle<T : QueueItem>(val nextItem: T?) : ProcessingState<T>()
    class ItemProcessing<T : QueueItem>(val item: T) : ProcessingState<T>()
    class ItemDone<T : QueueItem>(val item: T) : ProcessingState<T>()
    class ItemFailed<T : QueueItem>(val item: T, val error: ProcessingErrorEvent) : ProcessingState<T>()
    class ItemRetrying<T : QueueItem>(val item: T) : ProcessingState<T>()
    class ItemSkipped<T : QueueItem>(val item: T) : ProcessingState<T>()
    class ItemAborted<T : QueueItem>(val item: T) : ProcessingState<T>()
    class QueueCanceled<T : QueueItem> : ProcessingState<T>()
    class QueueDone<T : QueueItem> : ProcessingState<T>()
    class QueueAborted<T : QueueItem> : ProcessingState<T>()
}
```

### Observable Flows
The queue manager provides several reactive flows:

```kotlin
// Current queue state (list of items)
val queueState: StateFlow<List<T>>

// Current processing state
val processingState: StateFlow<ProcessingState<T>>

// Processor-specific events
val processorEvents: SharedFlow<E>

// Queue-level input requests
val queueInputRequests: SharedFlow<QueueInputRequest>
```

## Methods

### Core Operations
```kotlin
// Add item to queue
suspend fun enqueue(item: T)

// Start processing (if startMode is MANUAL)
suspend fun startProcessing()

// Abort current item or all items
suspend fun abort(item: T? = null)

// Clear all items from queue
suspend fun clear()

// Retry failed item
suspend fun retry(item: T)

// Skip current item (move to end)
suspend fun skip(item: T)
```

### Input Handling
```kotlin
// Provide response to queue-level input request
suspend fun provideQueueInput(response: QueueInputResponse)
```

### Item Management
```kotlin
// Get current item being processed
fun getCurrentItem(): T?

// Replace current item with modified version
suspend fun replaceCurrentItem(item: T)
```

## Thread Safety

The `HybridQueueManager` is designed to be thread-safe:

- Uses coroutines with proper synchronization
- All operations are suspend functions
- StateFlow and SharedFlow provide thread-safe reactive streams
- Internal state is protected with appropriate concurrency controls

## Best Practices

1. **Use appropriate persistence strategy**: Choose based on your durability requirements
2. **Handle all processing states**: Implement UI reactions for all possible states
3. **Provide input responses**: Always respond to input requests to avoid blocking
4. **Use proper coroutine scope**: Provide appropriate scope for lifecycle management
5. **Monitor queue state**: Use reactive flows to keep UI in sync with queue state

## Example Implementation

```kotlin
class PaymentQueueViewModel : ViewModel() {
    private val paymentQueue = HybridQueueManager(
        storage = ProcessingPaymentStorage(dao),
        processor = DynamicPaymentProcessor(),
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode = ProcessorStartMode.IMMEDIATE,
        scope = viewModelScope
    )
    
    init {
        observeProcessingState()
        observeQueueInputRequests()
    }
    
    private fun observeProcessingState() {
        viewModelScope.launch {
            paymentQueue.processingState.collect { state ->
                when (state) {
                    is ProcessingState.ItemProcessing -> {
                        updateUI("Processing payment ${state.item.id}")
                    }
                    is ProcessingState.ItemDone -> {
                        updateUI("Payment completed")
                    }
                    is ProcessingState.ItemFailed -> {
                        updateUI("Payment failed: ${state.error}")
                    }
                    // Handle other states...
                }
            }
        }
    }
    
    private fun observeQueueInputRequests() {
        viewModelScope.launch {
            paymentQueue.queueInputRequests.collect { request ->
                when (request) {
                    is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
                        showErrorDialog(request.error) { action ->
                            val response = when (action) {
                                ErrorHandlingAction.RETRY -> QueueInputResponse.onErrorRetry(request.id)
                                ErrorHandlingAction.SKIP -> QueueInputResponse.onErrorSkip(request.id)
                                ErrorHandlingAction.ABORT -> QueueInputResponse.onErrorAbort(request.id)
                                ErrorHandlingAction.ABORT_ALL -> QueueInputResponse.onErrorAbortAll(request.id)
                            }
                            paymentQueue.provideQueueInput(response)
                        }
                    }
                    // Handle other requests...
                }
            }
        }
    }
}
```
