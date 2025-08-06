# Payment Events

Comprehensive guide to payment-specific events emitted during payment processing.

## Overview

Payment processors emit events throughout the payment lifecycle to provide real-time feedback to the UI. These events extend the `BaseProcessingEvent` interface and are specific to payment operations.

## Event Hierarchy

```kotlin
sealed class ProcessingPaymentEvent : BaseProcessingEvent {
    // Lifecycle events
    object START : ProcessingPaymentEvent()
    object TRANSACTION_DONE : ProcessingPaymentEvent()
    object CANCELLED : ProcessingPaymentEvent()
    
    // Card interaction events
    object CARD_REACH_OR_INSERT : ProcessingPaymentEvent()
    object PIN_REQUESTED : ProcessingPaymentEvent()
    
    // Transaction processing events
    object TRANSACTION_PROCESSING : ProcessingPaymentEvent()
    object APPROVAL_SUCCEEDED : ProcessingPaymentEvent()
    object APPROVAL_DECLINED : ProcessingPaymentEvent()
    
    // Receipt printing events
    object PRINTING_RECEIPT : ProcessingPaymentEvent()
    
    // Error events
    object GENERIC_ERROR : ProcessingPaymentEvent()
    object GENERIC_SUCCESS : ProcessingPaymentEvent()
}
```

## Event Details

### Lifecycle Events

#### START
Emitted when payment processing begins.

```kotlin
object START : ProcessingPaymentEvent()
```

**When emitted**: At the beginning of `processPayment()`
**UI Action**: Show "Payment started" message, initialize progress indicators

#### TRANSACTION_DONE
Emitted when payment processing is complete (success or failure).

```kotlin
object TRANSACTION_DONE : ProcessingPaymentEvent()
```

**When emitted**: At the end of payment processing
**UI Action**: Hide progress indicators, show final result

#### CANCELLED
Emitted when payment processing is cancelled by user or system.

```kotlin
object CANCELLED : ProcessingPaymentEvent()
```

**When emitted**: When `abort()` is called or user cancels
**UI Action**: Show cancellation message, reset UI state

### Card Interaction Events

#### CARD_REACH_OR_INSERT
Emitted when the system is ready for card interaction.

```kotlin
object CARD_REACH_OR_INSERT : ProcessingPaymentEvent()
```

**When emitted**: After transaction initialization, waiting for card
**UI Action**: Show "Please insert or tap your card" message with card animation

#### PIN_REQUESTED
Emitted when PIN entry is required (debit cards).

```kotlin
object PIN_REQUESTED : ProcessingPaymentEvent()
```

**When emitted**: During debit card processing when PIN is required
**UI Action**: Show "Please enter your PIN" message

### Transaction Processing Events

#### TRANSACTION_PROCESSING
Emitted when the actual transaction is being processed by the acquirer.

```kotlin
object TRANSACTION_PROCESSING : ProcessingPaymentEvent()
```

**When emitted**: After card data is captured, during acquirer communication
**UI Action**: Show "Processing transaction..." with spinner/progress animation

#### APPROVAL_SUCCEEDED
Emitted when the transaction is approved by the acquirer.

```kotlin
object APPROVAL_SUCCEEDED : ProcessingPaymentEvent()
```

**When emitted**: When acquirer approves the transaction
**UI Action**: Show success animation, "Payment approved" message

#### APPROVAL_DECLINED
Emitted when the transaction is declined by the acquirer.

```kotlin
object APPROVAL_DECLINED : ProcessingPaymentEvent()
```

**When emitted**: When acquirer declines the transaction
**UI Action**: Show error animation, "Payment declined" message

### Receipt Printing Events

#### PRINTING_RECEIPT
Emitted when receipt printing is in progress.

```kotlin
object PRINTING_RECEIPT : ProcessingPaymentEvent()
```

**When emitted**: During customer or merchant receipt printing
**UI Action**: Show "Printing receipt..." message

### Error Events

#### GENERIC_ERROR
Emitted for general payment processing errors.

```kotlin
object GENERIC_ERROR : ProcessingPaymentEvent()
```

**When emitted**: When an unspecified error occurs during processing
**UI Action**: Show generic error message

#### GENERIC_SUCCESS
Emitted for general payment processing success.

```kotlin
object GENERIC_SUCCESS : ProcessingPaymentEvent()
```

**When emitted**: When payment completes successfully without specific success event
**UI Action**: Show generic success message

## Event Flow Examples

### Successful Credit Card Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
TRANSACTION_PROCESSING
↓
APPROVAL_SUCCEEDED
↓
PRINTING_RECEIPT (if receipt requested)
↓
TRANSACTION_DONE
```

### Successful Debit Card Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
PIN_REQUESTED
↓
TRANSACTION_PROCESSING
↓
APPROVAL_SUCCEEDED
↓
PRINTING_RECEIPT (if receipt requested)
↓
TRANSACTION_DONE
```

### Declined Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
TRANSACTION_PROCESSING
↓
APPROVAL_DECLINED
↓
TRANSACTION_DONE
```

### Cancelled Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
CANCELLED (user cancels)
```

## UI Implementation Examples

### Basic Event Handling
```kotlin
fun observePaymentEvents() {
    viewModelScope.launch {
        paymentQueue.processorEvents.collect { event ->
            when (event) {
                ProcessingPaymentEvent.START -> {
                    updateUI("Payment started")
                    showProgressIndicator(true)
                }
                
                ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> {
                    updateUI("Please insert or tap your card")
                    showCardAnimation()
                }
                
                ProcessingPaymentEvent.PIN_REQUESTED -> {
                    updateUI("Please enter your PIN")
                    showPinAnimation()
                }
                
                ProcessingPaymentEvent.TRANSACTION_PROCESSING -> {
                    updateUI("Processing transaction...")
                    showProcessingAnimation()
                }
                
                ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> {
                    updateUI("Payment approved!")
                    showSuccessAnimation()
                    playSuccessSound()
                }
                
                ProcessingPaymentEvent.APPROVAL_DECLINED -> {
                    updateUI("Payment declined")
                    showErrorAnimation()
                    playErrorSound()
                }
                
                ProcessingPaymentEvent.PRINTING_RECEIPT -> {
                    updateUI("Printing receipt...")
                    showPrintingAnimation()
                }
                
                ProcessingPaymentEvent.TRANSACTION_DONE -> {
                    showProgressIndicator(false)
                    // Transaction complete - show final state
                }
                
                ProcessingPaymentEvent.CANCELLED -> {
                    updateUI("Payment cancelled")
                    showProgressIndicator(false)
                    resetUI()
                }
                
                ProcessingPaymentEvent.GENERIC_ERROR -> {
                    updateUI("Payment error occurred")
                    showErrorAnimation()
                }
                
                ProcessingPaymentEvent.GENERIC_SUCCESS -> {
                    updateUI("Payment completed successfully")
                    showSuccessAnimation()
                }
            }
        }
    }
}
```

### Advanced Event Handling with State Management
```kotlin
data class PaymentUiState(
    val status: String = "",
    val isProcessing: Boolean = false,
    val showCardAnimation: Boolean = false,
    val showPinAnimation: Boolean = false,
    val showProcessingAnimation: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val showErrorAnimation: Boolean = false,
    val showPrintingAnimation: Boolean = false
)

class PaymentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()
    
    private fun handlePaymentEvent(event: ProcessingPaymentEvent) {
        _uiState.value = when (event) {
            ProcessingPaymentEvent.START -> _uiState.value.copy(
                status = "Payment started",
                isProcessing = true,
                showCardAnimation = false,
                showSuccessAnimation = false,
                showErrorAnimation = false
            )
            
            ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> _uiState.value.copy(
                status = "Please insert or tap your card",
                showCardAnimation = true,
                showPinAnimation = false,
                showProcessingAnimation = false
            )
            
            ProcessingPaymentEvent.PIN_REQUESTED -> _uiState.value.copy(
                status = "Please enter your PIN",
                showCardAnimation = false,
                showPinAnimation = true
            )
            
            ProcessingPaymentEvent.TRANSACTION_PROCESSING -> _uiState.value.copy(
                status = "Processing transaction...",
                showCardAnimation = false,
                showPinAnimation = false,
                showProcessingAnimation = true
            )
            
            ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> _uiState.value.copy(
                status = "Payment approved!",
                showProcessingAnimation = false,
                showSuccessAnimation = true
            )
            
            ProcessingPaymentEvent.APPROVAL_DECLINED -> _uiState.value.copy(
                status = "Payment declined",
                showProcessingAnimation = false,
                showErrorAnimation = true
            )
            
            ProcessingPaymentEvent.PRINTING_RECEIPT -> _uiState.value.copy(
                status = "Printing receipt...",
                showSuccessAnimation = false,
                showPrintingAnimation = true
            )
            
            ProcessingPaymentEvent.TRANSACTION_DONE -> _uiState.value.copy(
                isProcessing = false,
                showPrintingAnimation = false
            )
            
            ProcessingPaymentEvent.CANCELLED -> _uiState.value.copy(
                status = "Payment cancelled",
                isProcessing = false,
                showCardAnimation = false,
                showPinAnimation = false,
                showProcessingAnimation = false,
                showSuccessAnimation = false,
                showErrorAnimation = true
            )
            
            ProcessingPaymentEvent.GENERIC_ERROR -> _uiState.value.copy(
                status = "Payment error occurred",
                showProcessingAnimation = false,
                showErrorAnimation = true
            )
            
            ProcessingPaymentEvent.GENERIC_SUCCESS -> _uiState.value.copy(
                status = "Payment completed successfully",
                showProcessingAnimation = false,
                showSuccessAnimation = true
            )
        }
    }
}
```

## Best Practices

### 1. Event Timing
Emit events at the right moments:
```kotlin
override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
    _events.emit(ProcessingPaymentEvent.START) // At the beginning
    
    try {
        _events.emit(ProcessingPaymentEvent.CARD_REACH_OR_INSERT) // Before waiting for card
        
        val cardData = waitForCard()
        
        _events.emit(ProcessingPaymentEvent.TRANSACTION_PROCESSING) // Before acquirer call
        
        val result = processWithAcquirer(cardData)
        
        if (result.isApproved) {
            _events.emit(ProcessingPaymentEvent.APPROVAL_SUCCEEDED) // Immediately after approval
        } else {
            _events.emit(ProcessingPaymentEvent.APPROVAL_DECLINED) // Immediately after decline
        }
        
        return result
        
    } finally {
        _events.emit(ProcessingPaymentEvent.TRANSACTION_DONE) // Always at the end
    }
}
```

### 2. Error Event Handling
Use specific events when possible, fall back to generic events:
```kotlin
catch (e: CardReadException) {
    // Use specific error handling if available
    return ProcessingResult.Error(ProcessingErrorEvent.CARD_READ_ERROR)
} catch (e: Exception) {
    _events.emit(ProcessingPaymentEvent.GENERIC_ERROR)
    return ProcessingResult.Error(ProcessingErrorEvent.GENERIC_ERROR)
}
```

### 3. UI Responsiveness
Handle events immediately to provide responsive UI:
```kotlin
// Use immediate UI updates
fun observePaymentEvents() {
    paymentQueue.processorEvents
        .onEach { event -> handleEventImmediately(event) }
        .launchIn(viewModelScope)
}

private fun handleEventImmediately(event: ProcessingPaymentEvent) {
    // Update UI state synchronously
    updateUIState(event)
    
    // Trigger animations or sounds asynchronously
    viewModelScope.launch {
        triggerAnimations(event)
    }
}
```

### 4. Event Logging
Log events for debugging and analytics:
```kotlin
private fun emitEvent(event: ProcessingPaymentEvent) {
    Log.d("PaymentProcessor", "Emitting event: $event")
    _events.emit(event)
    
    // Optional: Send to analytics
    analytics.trackPaymentEvent(event)
}
```
