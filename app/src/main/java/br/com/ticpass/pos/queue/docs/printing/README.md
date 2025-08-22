# Printing Queue System

Printing-specific implementation of the generic queue management system for handling print jobs from files.

**Supported Operations**: File-based printing with support for multiple printer types including acquirer SDKs and network printers.

## Quick Start

```kotlin
// Create a printing queue
val printingQueue = PrintingQueueFactory().createDynamicPrintingQueue(
    storage = PrintingStorage(dao),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.IMMEDIATE,
    scope = viewModelScope
)

// Start processing (required - queue doesn't auto-start)
printingQueue.startProcessing()

// Enqueue a print job
val printJob = PrintingQueueItem(
    id = UUID.randomUUID().toString(),
    filePath = "/path/to/receipt.png",
    processorType = PrintingProcessorType.ACQUIRER
)
printingQueue.enqueue(printJob)
```

## Printing Queue Components

### PrintingQueueItem
Printing-specific queue item:

```kotlin
data class PrintingQueueItem(
    override val id: String = UUID.randomUUID().toString(),
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val filePath: String,
    val processorType: PrintingProcessorType,
) : QueueItem
```

### PrintingProcessorType
Defines the types of printing processors available:

```kotlin
enum class PrintingProcessorType {
    ACQUIRER,    // Uses acquirer SDK for printing
    MP_4200_HS,  // Network printer MP 4200 HS
    MPT_II,      // MPT II printer
}
```

### DynamicPrintingProcessor
Handles different printing operations through processor delegation:

```kotlin
class DynamicPrintingProcessor(
    private val processorMap: Map<PrintingProcessorType, PrintingProcessorBase>
) : PrintingProcessorBase() {
    // Delegates to appropriate processor based on item's processorType
    // Forwards events and input requests from delegate processors
    // Supports ACQUIRER, MP_4200_HS, and MPT_II printing
}
```

### Printing Events

```kotlin
sealed class PrintingEvent : BaseProcessingEvent {
    /**
     * Printing processing has started.
     */
    object START : PrintingEvent()

    /**
     * Printing process was canceled by user or system.
     */
    object CANCELLED : PrintingEvent()

    /**
     * Printing is being processed.
     */
    object PROCESSING : PrintingEvent()

    /**
     * Printing is currently in progress.
     */
    object PRINTING : PrintingEvent()
}
```

## Processing Results

Printing operations return specific result types:

```kotlin
// Success result
class PrintingSuccess : ProcessingResult.Success()

// Error result with specific error event
class PrintingError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)
```

## Usage Example

```kotlin
// Observing printing events
printingQueue.events.collect { event ->
    when (event) {
        is PrintingEvent.START -> {
            showProcessingDialog("Starting print job...")
        }
        is PrintingEvent.PROCESSING -> {
            updateProcessingDialog("Preparing document...")
        }
        is PrintingEvent.PRINTING -> {
            updateProcessingDialog("Printing document...")
        }
        is PrintingEvent.CANCELLED -> {
            dismissProcessingDialog()
            showMessage("Printing cancelled")
        }
    }
}

// Handling user input requests (for network printers)
printingQueue.userInputRequests.collect { request ->
    when (request.type) {
        UserInputRequest.Type.CONFIRM_PRINTER_NETWORK_INFO -> {
            val networkInfo = showNetworkInfoDialog()
            printingQueue.provideUserInput(
                UserInputResponse.confirmed(request.id, networkInfo)
            )
        }
    }
}
```

## Processor Types

### ACQUIRER Processor
- **Stone**: Uses `PosPrintProvider` with bitmap scaling for receipt paper
- **PagSeguro**: Uses `PlugPag.printFromFile()` with `PlugPagPrinterData`
- **File Support**: Processes image files (PNG, JPG) from file paths
- **Auto-scaling**: Stone processor automatically scales images to 384px width

### MP_4200_HS Processor
- **Network Printing**: Uses ESCPOSPrinter over TCP/IP
- **User Input**: Requests network info (IP address and port) from user
- **Default Config**: Falls back to 192.168.0.2:9100 if no input provided
- **Bitmap Processing**: Reads and processes bitmap files directly

### MPT_II Processor
- **Basic Implementation**: Currently returns success without actual printing
- **Extensible**: Ready for future MPT II printer integration

## Error Handling

Printing errors are handled through the generic error system:

- `PRINT_FILE_NOT_FOUND` - File path doesn't exist or unreadable
- `GENERIC` - General printing errors
- Acquirer-specific errors mapped through error translation system

## Advanced Features

- **Dynamic Processor Selection**: Automatically delegates to correct processor based on `processorType`
- **Multi-Acquirer Support**: Seamless printing via PagSeguro and Stone SDKs
- **Network Printer Support**: Direct TCP/IP printing to network printers
- **File-Based Printing**: Processes pre-generated receipt images
- **Interactive Processing**: User input requests for network configuration
- **Event-Driven Design**: Real-time feedback through event emission
- **Error Recovery**: Comprehensive error handling and resource cleanup
- **Resource Management**: Automatic bitmap recycling and coroutine cleanup

## Related Documentation

- [Printing Events](events.md)
- [Generic Queue System](../README.md)
- [Error Handling](../error-handling.md)
