# Print Queue System

Print-specific implementation of the generic queue management system for handling print jobs and receipt printing.

## Quick Start

```kotlin
// Create a print queue
val printQueue = PrintQueueFactory().createPrintQueue(
    storage = PrintStorage(dao),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.MANUAL
)

// Enqueue a print job
val printJob = PrintQueueItem(
    id = UUID.randomUUID().toString(),
    content = "Receipt content...",
    printerType = PrinterType.THERMAL,
    copies = 1
)
printQueue.enqueue(printJob)

// Start processing
printQueue.startProcessing()
```

## Print Queue Components

### PrintQueueItem
Print-specific queue item:

```kotlin
data class PrintQueueItem(
    override val id: String,
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val content: String,
    val printerType: PrinterType,
    val copies: Int = 1
) : QueueItem
```

### PrintProcessor
Handles print job processing:

```kotlin
class PrintProcessor : QueueProcessor<PrintQueueItem, PrintingEvent> {
    // Processes print jobs
    // Handles printer communication
    // Emits printing events
}
```

### Printing Events

```kotlin
sealed class PrintingEvent : BaseProcessingEvent {
    object PRINT_STARTED : PrintingEvent()
    object PRINT_COMPLETED : PrintingEvent()
    object PRINTER_ERROR : PrintingEvent()
    object PAPER_OUT : PrintingEvent()
    // ... other events
}
```

## Usage Example

```kotlin
fun observePrintEvents() {
    printQueue.processorEvents.collect { event ->
        when (event) {
            PrintingEvent.PRINT_STARTED -> {
                updateUI("Printing started...")
            }
            PrintingEvent.PRINT_COMPLETED -> {
                updateUI("Print completed")
            }
            PrintingEvent.PRINTER_ERROR -> {
                updateUI("Printer error")
            }
            PrintingEvent.PAPER_OUT -> {
                updateUI("Paper out - please refill")
            }
        }
    }
}
```

## Related Documentation

- [Generic Queue System](../README.md)
- [Print Examples](examples.md)
