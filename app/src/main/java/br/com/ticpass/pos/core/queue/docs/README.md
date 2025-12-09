# Queue Management System

A generic, type-safe queue management system that combines in-memory operations with optional persistence strategies.

**Current Implementations**: Payment processing, NFC operations, printing jobs, and refund processing.

## Quick Start

```kotlin
// Create a queue manager
val queueManager = HybridQueueManager(
    storage = YourQueueStorage(),
    processor = YourQueueProcessor(),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.IMMEDIATE
)

// Enqueue items
queueManager.enqueue(yourQueueItem)

// Observe processing
queueManager.processingState.collect { state ->
    when (state) {
        is ProcessingState.ItemProcessing -> { /* Item being processed */ }
        is ProcessingState.ItemDone -> { /* Item completed */ }
        is ProcessingState.ItemFailed -> { /* Handle error */ }
    }
}
```

## Core Components

### QueueItem
Basic unit of work that must be implemented by all queue items:

```kotlin
interface QueueItem {
    val id: String
    val priority: Int
    var status: QueueItemStatus
}
```

### QueueProcessor
Processes queue items and emits events:

```kotlin
interface QueueProcessor<T : QueueItem, E : BaseProcessingEvent> {
    val events: SharedFlow<E>
    val userInputRequests: SharedFlow<UserInputRequest>
    
    suspend fun process(item: T): ProcessingResult
    suspend fun provideUserInput(response: UserInputResponse)
    suspend fun abort(item: T? = null): Boolean
}
```

### QueueStorage
Handles persistence of queue items:

```kotlin
interface QueueStorage<T : QueueItem> {
    suspend fun insert(item: T)
    suspend fun update(item: T)
    suspend fun remove(item: T)
    suspend fun getNextPending(): T?
    suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<T>
    // ... other methods
}
```

### HybridQueueManager
Core queue management class that orchestrates processing:

```kotlin
class HybridQueueManager<T : QueueItem, E : BaseProcessingEvent>(
    private val storage: QueueStorage<T>,
    internal val processor: QueueProcessor<T, E>,
    private val persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
    val startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE
)
```

## Key Features

- **Type Safety**: Generic interfaces ensure compile-time type safety
- **Reactive Design**: Kotlin Flow integration for state updates
- **Flexible Persistence**: Choose when and how items are persisted
- **Interactive Processing**: Support for user input during processing
- **Error Handling**: Comprehensive error handling with retry/skip/abort options
- **Processor Agnostic**: Works with any queue item type (payments, NFC, printing, refunds)

## Configuration Options

### PersistenceStrategy
```kotlin
enum class PersistenceStrategy {
    IMMEDIATE,  // Save immediately when items are added/modified
    NEVER       // In-memory only
}
```

### ProcessorStartMode
```kotlin
enum class ProcessorStartMode {
    IMMEDIATE,  // Start processing when items are enqueued
    MANUAL      // Wait for explicit startProcessing() call
}
```

## Processing States

The queue manager emits different processing states:

```kotlin
sealed class ProcessingState<T : QueueItem> {
    class QueueIdle<T : QueueItem>(val nextItem: T?) : ProcessingState<T>()
    class ItemProcessing<T : QueueItem>(val item: T) : ProcessingState<T>()
    class ItemDone<T : QueueItem>(val item: T) : ProcessingState<T>()
    class ItemFailed<T : QueueItem>(val item: T, val error: ProcessingErrorEvent) : ProcessingState<T>()
    // ... other states
}
```

## Input Handling

The system supports two types of input requests:

### Queue-Level Input Requests
Handled by the queue manager for queue operations:

```kotlin
sealed class QueueInputRequest {
    data class CONFIRM_NEXT_PROCESSOR(...) : QueueInputRequest()
    data class ERROR_RETRY_OR_SKIP(...) : QueueInputRequest()
}
```

### User Input Requests
Handled by processors for domain-specific input:

```kotlin
sealed class UserInputRequest {
    // Implement your specific input request types
}
```

## Implementation Examples

- **Payments**: See [Payment Queue Documentation](payments/README.md)
- **NFC Operations**: See [NFC Queue Documentation](nfc/README.md)
- **Printing**: See [Print Queue Documentation](printing/README.md)
- **Refund Processing**: See [Refund Queue Documentation](refund/README.md)

## Advanced Topics

- **Core Components**: [queue-manager.md](core/queue-manager.md)
- **Persistence Strategies**: [persistence.md](core/persistence.md)
- **Input Handling**: [input-handling.md](core/input-handling.md)

## Benefits

1. **Hybrid Performance**: Fast in-memory operations with optional persistence
2. **Type Safety**: Generic interfaces prevent runtime errors
3. **Reactive Design**: Real-time state updates with Kotlin Flows
4. **Interactive Processing**: Built-in support for user interaction
5. **Extensibility**: Easy to add new queue types and processors
6. **Error Resilience**: Comprehensive error handling and recovery options
