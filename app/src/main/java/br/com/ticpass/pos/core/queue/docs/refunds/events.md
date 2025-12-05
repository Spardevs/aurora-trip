# Refund Events

Comprehensive guide to refund-specific events emitted during refund processing.

## Overview

Refund processors emit events throughout the refund lifecycle to provide real-time feedback to the UI. These events extend the `BaseProcessingEvent` interface and are specific to refund operations.

## Event Hierarchy

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

## Event Flow

The typical refund event flow follows this sequence:

1. **START** - Refund processing begins
2. **PROCESSING** - Initial processing phase
3. **REFUNDING** - Actual refund transaction in progress
4. **SUCCESS** - Refund completed successfully

Alternative flows:
- **CANCELLED** - Can occur at any point if user or system cancels the refund

## Event Details

### Lifecycle Events

#### START
- **When**: Refund processing begins
- **Purpose**: Indicates the refund processor has started working on the refund request
- **UI Impact**: Show initial processing state

#### PROCESSING
- **When**: Initial refund validation and setup phase
- **Purpose**: Indicates the processor is preparing the refund transaction
- **UI Impact**: Display processing indicator

#### REFUNDING
- **When**: Actual refund transaction is being executed
- **Purpose**: The refund is being processed by the acquirer
- **UI Impact**: Show refund in progress state

#### SUCCESS
- **When**: Refund transaction completed successfully
- **Purpose**: Indicates successful refund completion
- **UI Impact**: Show success state and allow user to continue

#### CANCELLED
- **When**: Refund process is cancelled by user or system
- **Purpose**: Indicates refund was aborted before completion
- **UI Impact**: Return to previous state or show cancellation message

## Usage in UI

```kotlin
// Observing refund events
refundQueue.events.collect { event ->
    when (event) {
        is RefundEvent.START -> {
            showProcessingDialog("Starting refund...")
        }
        is RefundEvent.PROCESSING -> {
            updateProcessingDialog("Processing refund...")
        }
        is RefundEvent.REFUNDING -> {
            updateProcessingDialog("Refunding transaction...")
        }
        is RefundEvent.SUCCESS -> {
            dismissProcessingDialog()
            showSuccessMessage("Refund completed successfully")
        }
        is RefundEvent.CANCELLED -> {
            dismissProcessingDialog()
            showMessage("Refund cancelled")
        }
    }
}
```

## Error Handling

Refund errors are handled through the generic error system rather than specific refund events. See the main queue documentation for error handling patterns.

## Related Documentation

- [Refund Queue Overview](README.md)
- [Main Queue Documentation](../README.md)
- [Error Handling](../error-handling.md)
