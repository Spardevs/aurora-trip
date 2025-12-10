# Queue Management System

A generic, type-safe queue management system that combines in-memory operations with optional persistence strategies. Designed to handle any type of processing workflow with reactive state management and comprehensive error handling.

**Current Implementations**: Payment processing, NFC operations, printing jobs, and refund processing.

## Quick Start

```kotlin
// Create a queue manager
val queueManager = HybridQueueManager(
    storage = YourQueueStorage(),
    processor = YourQueueProcessor(),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.CONFIRMATION
)

// Enqueue items
queueManager.enqueue(yourQueueItem)

// Start processing (required - queue won't start automatically)
queueManager.startProcessing()

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
Base interface for all items that can be processed by the queue system. Provides unique identification, priority ordering, and status tracking.

```kotlin
interface QueueItem {
    val id: String
    val priority: Int
    var status: QueueItemStatus
}
```

### QueueProcessor
Defines the processing logic for specific queue item types. Handles the actual work, emits events, and manages user interactions during processing.

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
Core queue management class that orchestrates item processing, persistence, and state management. Combines in-memory operations with configurable persistence strategies.

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
- **IMMEDIATE**: No confirmation input request before processing items (startProcessing() still required)
- **CONFIRMATION**: Wait for confirmation input request before proceeding to process each item

### Processor-Agnostic Design
The `HybridQueueManager` is completely processor-agnostic:
- No payment-specific logic in core queue management
- Generic confirmation flow works for any queue item type
- Item-specific modifications handled at UseCase layer
- Reusable for any processing type (payments, NFC, printing, refunds)

### Processing Results
```kotlin
sealed class ProcessingResult {
    abstract class Success : ProcessingResult()
    abstract class Error(val event: ProcessingErrorEvent) : ProcessingResult()
}
```

**Specialized Results**: Each processor type has its own concrete implementations:
- `PaymentProcessingResult.Success(atk, txId)`
- `NFCProcessingResult.Success(tagData)`
- `PrintingProcessingResult.Success()`
- `RefundProcessingResult.Success(refundId)`

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

### NFC Operations
NFC tag reading and writing operations with MIFARE Classic support.

📖 **[NFC Queue Documentation](docs/nfc/README.md)**

### Print Jobs
Print job queue system for handling receipt and document printing.

📖 **[Print Queue Documentation](docs/printing/README.md)**

### Refund Processing
Refund processing system for handling transaction reversals.

📖 **[Refund Queue Documentation](docs/refunds/README.md)**

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
- **[NFC Operations](docs/nfc/README.md)** - NFC tag operations and utilities
- **[Refund Processing](docs/refunds/README.md)** - Refund processor implementation

## Benefits
1
1. **Hybrid Performance**: Fast in-memory operations with optional persistence
2. **Type Safety**: Generic interfaces prevent runtime type errors
3. **Reactive Design**: Real-time state updates with Kotlin Flows
4. **Interactive Processing**: Built-in support for user interaction workflows
5. **Extensibility**: Easy to add new queue types and processors
6. **Error Resilience**: Comprehensive error handling and recovery options
7. **Processor Agnostic**: Reusable for any type of processing workflow

## Getting Started

1. **Choose your implementation**: Start with [Payment Queue](docs/payments/README.md), [Print Queue](docs/printing/README.md), or explore NFC/Refund processors
2. **Read the core concepts**: Review [Generic Queue System](docs/README.md)
3. **Implement your processor**: Follow the processor interface guidelines
4. **Handle user input**: Use the [Input Handling Guide](docs/core/input-handling.md)
5. **Test and iterate**: Use the comprehensive state management for debugging

## Current Processor Types

- **Payment**: Card transactions, PIX, cash payments with receipt printing
- **NFC**: MIFARE Classic tag reading/writing with comprehensive tag mapping
- **Printing**: Receipt and document printing with various printer types
- **Refund**: Transaction reversal processing with proper error handling

## Creating New Queue Types

The system is designed to be easily extensible. To create a new queue type:

1. **Define your QueueItem**: Implement the `QueueItem` interface
2. **Create your processor**: Implement `QueueProcessor<YourItem, YourEvent>`
3. **Define your events**: Extend `BaseProcessingEvent` for your domain
4. **Add user input requests**: Define domain-specific `UserInputRequest` types
5. **Create documentation**: Follow the structure in `docs/your-queue-type/`

The modular documentation structure makes it easy to maintain and extend as new queue types are added to the system.
