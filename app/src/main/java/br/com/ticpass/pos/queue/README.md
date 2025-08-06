# Queue Management System

A generic, type-safe queue management system that combines in-memory operations with optional persistence strategies. Designed to handle any type of processing workflow with reactive state management and comprehensive error handling.

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

// Observe processing states
queueManager.processingState.collect { state ->
    when (state) {
        is ProcessingState.ItemProcessing -> { /* Item being processed */ }
        is ProcessingState.ItemDone -> { /* Item completed successfully */ }
        is ProcessingState.ItemFailed -> { /* Handle processing error */ }
        is ProcessingState.QueueDone -> { /* All items completed */ }
    }
}
```

## Architecture Overview

The queue system is built with these core principles:

- **Type Safety**: Generic interfaces ensure compile-time type safety
- **Reactive Design**: Kotlin Flow integration for real-time state updates  
- **Flexible Persistence**: Choose when and how items are persisted
- **Interactive Processing**: Built-in support for user input during processing
- **Error Resilience**: Comprehensive error handling with retry/skip/abort options
- **Processor Agnostic**: Works with any queue item type and processing logic

## Core Components

### QueueItem
```kotlin
interface QueueItem {
    val id: String
    val priority: Int
    var status: QueueItemStatus
}
```

### QueueProcessor
```kotlin
interface QueueProcessor<T : QueueItem, E : BaseProcessingEvent> {
    val events: SharedFlow<E>
    val userInputRequests: SharedFlow<UserInputRequest>
    
    suspend fun process(item: T): ProcessingResult
    suspend fun provideUserInput(response: UserInputResponse)
    suspend fun abort(item: T? = null): Boolean
}
```

### HybridQueueManager
```kotlin
class HybridQueueManager<T : QueueItem, E : BaseProcessingEvent>(
    private val storage: QueueStorage<T>,
    internal val processor: QueueProcessor<T, E>,
    private val persistenceStrategy: PersistenceStrategy,
    val startMode: ProcessorStartMode
)
```

## Key Features

### Persistence Strategies
- **IMMEDIATE**: Save items immediately when added or modified
- **NEVER**: In-memory only processing

### Processor Start Modes
- **IMMEDIATE**: Start processing when items are enqueued
- **MANUAL**: Wait for explicit `startProcessing()` call

### Processing Results
```kotlin
sealed class ProcessingResult {
    class Success(val atk: String, val txId: String) : ProcessingResult()
    data class Error(val event: ProcessingErrorEvent) : ProcessingResult()
}
```

### Error Handling
When processing fails, the system provides these options:
- **RETRY**: Retry the same item immediately
- **SKIP**: Move item to end of queue for later processing
- **ABORT**: Remove current item from processing
- **ABORT_ALL**: Cancel entire queue processing

## Input Handling

The system supports two types of input requests:

### Queue-Level Input Requests
```kotlin
// Confirmation before processing next item
QueueInputRequest.CONFIRM_NEXT_PROCESSOR(...)

// Error handling decisions
QueueInputRequest.ERROR_RETRY_OR_SKIP(...)
```

### User Input Requests
Domain-specific input requests defined by each processor implementation.

## Implementation Examples

### Payment Processing
Complete payment processing system with card transactions, PIX, cash, and receipt printing.

📖 **[Payment Queue Documentation](docs/payments/README.md)**

### Print Jobs
Print job queue system for handling receipt and document printing.

📖 **[Print Queue Documentation](docs/printing/README.md)**

## Advanced Documentation

### Core System Details
- **[Generic Queue System](docs/README.md)** - Comprehensive generic queue documentation
- **[Input Handling Guide](docs/core/input-handling.md)** - Complete input request/response guide
- **[Queue Manager Details](docs/core/queue-manager.md)** - HybridQueueManager deep dive
- **[Persistence Strategies](docs/core/persistence.md)** - Storage and persistence options

### Implementation Guides
- **[Payment Processors](docs/payments/processors.md)** - Payment processor implementations
- **[Payment Events](docs/payments/events.md)** - Payment-specific events
- **[Payment Examples](docs/payments/examples.md)** - Complete payment usage examples

## Benefits

1. **Hybrid Performance**: Fast in-memory operations with optional persistence
2. **Type Safety**: Generic interfaces prevent runtime type errors
3. **Reactive Design**: Real-time state updates with Kotlin Flows
4. **Interactive Processing**: Built-in support for user interaction workflows
5. **Extensibility**: Easy to add new queue types and processors
6. **Error Resilience**: Comprehensive error handling and recovery options
7. **Processor Agnostic**: Reusable for any type of processing workflow

## Getting Started

1. **Choose your implementation**: Start with [Payment Queue](docs/payments/README.md) or [Print Queue](docs/printing/README.md)
2. **Read the core concepts**: Review [Generic Queue System](docs/README.md)
3. **Implement your processor**: Follow the processor interface guidelines
4. **Handle user input**: Use the [Input Handling Guide](docs/core/input-handling.md)
5. **Test and iterate**: Use the comprehensive state management for debugging

## Creating New Queue Types

The system is designed to be easily extensible. To create a new queue type:

1. **Define your QueueItem**: Implement the `QueueItem` interface
2. **Create your processor**: Implement `QueueProcessor<YourItem, YourEvent>`
3. **Define your events**: Extend `BaseProcessingEvent` for your domain
4. **Add user input requests**: Define domain-specific `UserInputRequest` types
5. **Create documentation**: Follow the structure in `docs/your-queue-type/`

The modular documentation structure makes it easy to maintain and extend as new queue types are added to the system.
