# NFC Queue System

NFC-specific implementation of the generic queue management system for handling NFC tag operations including customer authentication, tag formatting, and customer setup.

**Supported Operations**: Customer authentication, tag formatting with brute force options, and customer setup operations.

## Quick Start

```kotlin
// Create an NFC queue
val nfcQueue = NFCQueueFactory().createDynamicNFCQueue(
    storage = NFCStorage(dao),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
    startMode = ProcessorStartMode.IMMEDIATE,
    scope = viewModelScope
)

// Enqueue a customer authentication operation
val authItem = NFCQueueItem.CustomerAuthOperation(
    id = UUID.randomUUID().toString(),
    timeout = 15000L
)
nfcQueue.enqueue(authItem)

// Start processing (required - queue won't start automatically)
nfcQueue.startProcessing()

// Observe NFC events
nfcQueue.processorEvents.collect { event ->
    when (event) {
        NFCEvent.START -> { /* NFC processing started */ }
        NFCEvent.AUTHENTICATING_SECTORS -> { /* Authenticating sectors */ }
        NFCEvent.READING_TAG_CUSTOMER_DATA -> { /* Reading customer data */ }
        NFCEvent.PROCESSING_TAG_CUSTOMER_DATA -> { /* Processing data */ }
        is NFCEvent.REACH_TAG -> { /* User should place tag */ }
        // Handle other events...
    }
}
```

## NFC Queue Components

### NFCQueueItem
Sealed class representing different NFC operations:

```kotlin
sealed class NFCQueueItem : QueueItem {
    abstract val processorType: NFCProcessorType
    
    /**
     * NFC Auth operation with authentication-specific parameters
     */
    data class CustomerAuthOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.CUSTOMER_AUTH,
        val timeout: Long = 15000L // Auth timeout in milliseconds
    ) : NFCQueueItem()

    /**
     * NFC Format operation with format-specific parameters
     */
    data class TagFormatOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.TAG_FORMAT,
        val bruteForce: NFCBruteForce,
    ) : NFCQueueItem()
    
    /**
     * NFC Setup operation with setup-specific parameters
     */
    data class CustomerSetupOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.CUSTOMER_SETUP,
        val timeout: Long = 20000L // Setup timeout in milliseconds
    ) : NFCQueueItem()
}
```

### NFCProcessorType
Defines the types of NFC processors available:

```kotlin
enum class NFCProcessorType {
    CUSTOMER_AUTH,
    TAG_FORMAT,
    CUSTOMER_SETUP,
}
```

### NFCBruteForce
Defines brute force options for tag formatting:

```kotlin
enum class NFCBruteForce {
    /**
     * All owned keys and all known keys will be used, including brute force attempts.
     */
    FULL,

    /**
     * Only owned keys and list of known (and likely to succeed) keys will be used.
     */
    MOST_LIKELY,

    /**
     * Only owned keys will be used, no brute force attempt will be made.
     */
    NONE
}
```

### DynamicNFCProcessor
Handles different NFC operations through processor delegation:

```kotlin
class DynamicNFCProcessor(
    private val processorMap: Map<NFCProcessorType, NFCProcessorBase>
) : NFCProcessorBase() {
    // Delegates to appropriate processor based on item's processorType
    // Forwards events and input requests from delegate processors
    // Supports CUSTOMER_AUTH, TAG_FORMAT, and CUSTOMER_SETUP operations
}
```

## NFC Events

NFC processors emit specific events during processing:

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

## Processing Results

NFC processing returns specific result types:

```kotlin
sealed class NFCSuccess : ProcessingResult.Success() {
    /**
     * NFC Auth operation success with authentication details
     */
    class CustomerAuthSuccess(
        val id: String,
        val name: String,
        val nationalId: String,
        val phone: String,
    ) : NFCSuccess()
    
    /**
     * NFC Setup operation success with configuration details
     */
    class CustomerSetupSuccess(
        val id: String,
        val name: String,
        val nationalId: String,
        val phone: String,
        val pin: String,
    ) : NFCSuccess()
    
    /**
     * NFC Format operation success with reset details
     */
    class FormatSuccess() : NFCSuccess()
}

class NFCError(event: ProcessingErrorEvent) : ProcessingResult.Error(event)
```

## User Input Requests

NFC processors may request user input for various operations:

```kotlin
sealed class UserInputRequest {
    /**
     * Request to confirm NFC keys
     */
    data class CONFIRM_NFC_KEYS(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 5_000L, // 5 seconds default timeout
    ) : UserInputRequest()

    /**
     * Request to confirm NFC tag authentication with a PIN
     * @param pin The PIN to be used for NFC tag authentication
     */
    data class CONFIRM_NFC_TAG_AUTH(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 30_000L, // 30 seconds default timeout
        val pin: String
    ) : UserInputRequest()

    /**
     * Request to confirm NFC tag customer data
     */
    data class CONFIRM_NFC_TAG_CUSTOMER_DATA(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 300_000L, // 300 seconds default timeout
    ) : UserInputRequest()

    /**
     * Request to customer confirm they've saved the NFC tag PIN
     */
    data class CONFIRM_NFC_TAG_CUSTOMER_SAVE_PIN(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 90_000L, // 90 seconds default timeout
        val pin: String
    ) : UserInputRequest()
}
```

## Usage Examples

### Customer Authentication

```kotlin
class NFCViewModel : ViewModel() {
    private val nfcQueue = NFCQueueFactory().createDynamicNFCQueue(...)
    
    fun authenticateCustomer() {
        val authItem = NFCQueueItem.CustomerAuthOperation(
            timeout = 15000L
        )
        
        viewModelScope.launch {
            nfcQueue.enqueue(authItem)
        }
    }
    
    fun observeNFCEvents() {
        viewModelScope.launch {
            nfcQueue.processorEvents.collect { event ->
                when (event) {
                    NFCEvent.START -> {
                        updateUI("NFC processing started")
                    }
                    NFCEvent.AUTHENTICATING_SECTORS -> {
                        updateUI("Authenticating NFC sectors...")
                    }
                    NFCEvent.READING_TAG_CUSTOMER_DATA -> {
                        updateUI("Reading customer data...")
                    }
                    is NFCEvent.REACH_TAG -> {
                        updateUI("Please place NFC tag on reader")
                        showTagPlacementPrompt(event.timeoutMs)
                    }
                    NFCEvent.CANCELLED -> {
                        updateUI("NFC operation cancelled")
                    }
                }
            }
        }
    }
}
```

### Tag Formatting

```kotlin
fun formatNFCTag(bruteForceLevel: NFCBruteForce) {
    val formatItem = NFCQueueItem.TagFormatOperation(
        bruteForce = bruteForceLevel
    )
    
    viewModelScope.launch {
        nfcQueue.enqueue(formatItem)
    }
}
```

### Customer Setup

```kotlin
fun setupCustomerTag() {
    val setupItem = NFCQueueItem.CustomerSetupOperation(
        timeout = 20000L
    )
    
    viewModelScope.launch {
        nfcQueue.enqueue(setupItem)
    }
}
```

## Advanced Features

- **Dynamic Processor**: Uses `NFCProcessorRegistry` for flexible processor selection
- **Customer Operations**: Specialized authentication and setup operations for customer management
- **Tag Formatting**: Comprehensive tag formatting with configurable brute force options
- **Interactive Processing**: Built-in user input handling for PIN confirmations and customer data
- **Event-Driven**: Rich event system for real-time operation feedback
- **Timeout Management**: Configurable timeouts for different operation types
- **Error Recovery**: Comprehensive error handling with operation-specific recovery strategies

## Related Documentation

- [Generic Queue System](../README.md)
- [Payment Queue System](../payments/README.md)
- [Print Queue System](../printing/README.md)
- [NFC Examples](examples.md)
