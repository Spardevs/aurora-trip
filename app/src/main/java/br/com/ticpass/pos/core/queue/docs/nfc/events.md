# NFC Events

Comprehensive guide to NFC-specific events emitted during NFC processing.

## Overview

NFC processors emit events throughout the NFC operation lifecycle to provide real-time feedback to the UI. These events extend the `BaseProcessingEvent` interface and are specific to NFC operations including customer authentication, tag formatting, and customer setup.

## Event Hierarchy

```kotlin
sealed class NFCEvent : BaseProcessingEvent {
    /**
     * Processor is authenticating sectors of the NFC tag.
     */
    object AUTHENTICATING_SECTORS : NFCEvent()

    /**
     * NFC tag is being formatted.
     */
    object FORMATTING_TAG : NFCEvent()

    /**
     * NFC processing has started.
     */
    object START : NFCEvent()

    /**
     * NFC process was canceled by user or system.
     */
    object CANCELLED : NFCEvent()

    /**
     * NFC is being processed.
     */
    object PROCESSING : NFCEvent()

    /**
     * Validating sector keys for the NFC tag.
     */
    object VALIDATING_SECTOR_KEYS : NFCEvent()

    /**
     * Reading customer data from the NFC tag.
     */
    object READING_TAG_CUSTOMER_DATA : NFCEvent()

    /**
     * Processing tag customer data.
     */
    object PROCESSING_TAG_CUSTOMER_DATA : NFCEvent()

    /**
     * Saving customer data to the NFC tag.
     */
    object SAVING_TAG_CUSTOMER_DATA : NFCEvent()

    /**
     * User should place the NFC tag on the reader.
     */
    data class REACH_TAG(
        val timeoutMs: Long = 5000L,
    ) : NFCEvent()
}
```

## Event Flows by Operation Type

### Customer Authentication Flow

1. **START** - Authentication process begins
2. **REACH_TAG** - Prompt user to place tag on reader
3. **AUTHENTICATING_SECTORS** - Authenticating tag sectors
4. **VALIDATING_SECTOR_KEYS** - Validating access keys
5. **READING_TAG_CUSTOMER_DATA** - Reading customer information
6. **PROCESSING_TAG_CUSTOMER_DATA** - Processing retrieved data
7. **SUCCESS** (via processing result) - Authentication completed

### Tag Formatting Flow

1. **START** - Formatting process begins
2. **REACH_TAG** - Prompt user to place tag on reader
3. **AUTHENTICATING_SECTORS** - Authenticating tag sectors
4. **FORMATTING_TAG** - Writing new data structure to tag
5. **SUCCESS** (via processing result) - Formatting completed

### Customer Setup Flow

1. **START** - Setup process begins
2. **REACH_TAG** - Prompt user to place tag on reader
3. **AUTHENTICATING_SECTORS** - Authenticating tag sectors
4. **PROCESSING_TAG_CUSTOMER_DATA** - Processing customer data
5. **SAVING_TAG_CUSTOMER_DATA** - Writing customer data to tag
6. **SUCCESS** (via processing result) - Setup completed

## Event Details

### Lifecycle Events

#### START
- **When**: NFC processing begins
- **Purpose**: Indicates the NFC processor has started working
- **UI Impact**: Show initial processing state

#### PROCESSING
- **When**: General processing phase
- **Purpose**: Indicates ongoing NFC operations
- **UI Impact**: Display processing indicator

#### CANCELLED
- **When**: NFC process is cancelled by user or system
- **Purpose**: Indicates operation was aborted before completion
- **UI Impact**: Return to previous state or show cancellation message

### Tag Interaction Events

#### REACH_TAG
- **When**: User needs to place NFC tag on reader
- **Purpose**: Prompt user for tag interaction
- **Parameters**: `timeoutMs` - timeout duration (default 5000ms)
- **UI Impact**: Show "Place tag on reader" prompt with countdown

#### AUTHENTICATING_SECTORS
- **When**: Processor is authenticating tag sectors
- **Purpose**: Indicates sector authentication in progress
- **UI Impact**: Show authentication progress

#### VALIDATING_SECTOR_KEYS
- **When**: Validating access keys for tag sectors
- **Purpose**: Indicates key validation process
- **UI Impact**: Show validation progress

### Data Operation Events

#### FORMATTING_TAG
- **When**: Tag is being formatted with new structure
- **Purpose**: Indicates formatting operation in progress
- **UI Impact**: Show formatting progress

#### READING_TAG_CUSTOMER_DATA
- **When**: Reading customer information from tag
- **Purpose**: Indicates data reading operation
- **UI Impact**: Show reading progress

#### PROCESSING_TAG_CUSTOMER_DATA
- **When**: Processing retrieved or new customer data
- **Purpose**: Indicates data processing phase
- **UI Impact**: Show data processing state

#### SAVING_TAG_CUSTOMER_DATA
- **When**: Writing customer data to tag
- **Purpose**: Indicates data writing operation
- **UI Impact**: Show saving progress

## Usage in UI

```kotlin
// Observing NFC events
nfcQueue.events.collect { event ->
    when (event) {
        is NFCEvent.START -> {
            showProcessingDialog("Starting NFC operation...")
        }
        is NFCEvent.REACH_TAG -> {
            showTagPrompt("Place NFC tag on reader", event.timeoutMs)
        }
        is NFCEvent.AUTHENTICATING_SECTORS -> {
            updateProcessingDialog("Authenticating tag...")
        }
        is NFCEvent.VALIDATING_SECTOR_KEYS -> {
            updateProcessingDialog("Validating keys...")
        }
        is NFCEvent.FORMATTING_TAG -> {
            updateProcessingDialog("Formatting tag...")
        }
        is NFCEvent.READING_TAG_CUSTOMER_DATA -> {
            updateProcessingDialog("Reading customer data...")
        }
        is NFCEvent.PROCESSING_TAG_CUSTOMER_DATA -> {
            updateProcessingDialog("Processing data...")
        }
        is NFCEvent.SAVING_TAG_CUSTOMER_DATA -> {
            updateProcessingDialog("Saving customer data...")
        }
        is NFCEvent.CANCELLED -> {
            dismissProcessingDialog()
            showMessage("NFC operation cancelled")
        }
    }
}
```

## Timeout Handling

The `REACH_TAG` event includes a timeout parameter that indicates how long to wait for user interaction:

```kotlin
when (event) {
    is NFCEvent.REACH_TAG -> {
        startCountdownTimer(event.timeoutMs) {
            // Handle timeout
            nfcViewModel.cancelOperation()
        }
    }
}
```

## Error Handling

NFC errors are handled through the generic error system and processing results rather than specific NFC events. See the main queue documentation for error handling patterns.

## Related Documentation

- [NFC Queue Overview](README.md)
- [Main Queue Documentation](../README.md)
- [User Input Requests](../user-input.md)
- [Error Handling](../error-handling.md)
