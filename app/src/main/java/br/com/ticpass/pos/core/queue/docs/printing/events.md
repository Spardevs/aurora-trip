# Printing Events

Comprehensive guide to printing-specific events emitted during printing processing.

## Overview

Printing processors emit events throughout the printing lifecycle to provide real-time feedback to the UI. These events extend the `BaseProcessingEvent` interface and are specific to printing operations.

## Event Hierarchy

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

## Event Flow

The typical printing event flow follows this sequence:

1. **START** - Printing processing begins
2. **PROCESSING** - Initial processing phase (preparing print job)
3. **PRINTING** - Actual printing in progress
4. **SUCCESS** (via processing result) - Printing completed successfully

Alternative flows:
- **CANCELLED** - Can occur at any point if user or system cancels the printing

## Event Details

### Lifecycle Events

#### START
- **When**: Printing processing begins
- **Purpose**: Indicates the printing processor has started working on the print job
- **UI Impact**: Show initial processing state

#### PROCESSING
- **When**: Print job preparation and validation phase
- **Purpose**: Indicates the processor is preparing the document for printing
- **UI Impact**: Display processing indicator

#### PRINTING
- **When**: Actual printing operation is being executed
- **Purpose**: The document is being sent to and processed by the printer
- **UI Impact**: Show printing in progress state

#### CANCELLED
- **When**: Printing process is cancelled by user or system
- **Purpose**: Indicates printing was aborted before completion
- **UI Impact**: Return to previous state or show cancellation message

## Usage in UI

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
```

## Print Job Types

The printing system supports various types of print jobs:

- **Receipt Printing** - Transaction receipts and customer receipts
- **Report Printing** - Administrative reports and summaries
- **Test Printing** - Printer connectivity and functionality tests

Each print job type follows the same event flow but may have different processing times and requirements.

## Error Handling

Printing errors are handled through the generic error system rather than specific printing events. Common printing errors include:

- Printer out of paper
- Printer connection issues
- Print job formatting errors
- Hardware malfunctions

See the main queue documentation for error handling patterns.

## Related Documentation

- [Printing Queue Overview](README.md)
- [Main Queue Documentation](../README.md)
- [Error Handling](../error-handling.md)
