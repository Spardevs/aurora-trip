# Payment Events

Comprehensive guide to payment-specific events emitted during payment processing.

## Overview

Payment processors emit events throughout the payment lifecycle to provide real-time feedback to the UI. These events extend the `BaseProcessingEvent` interface and are specific to payment operations.

## Event Hierarchy

```kotlin
sealed class PaymentProcessingEvent : BaseProcessingEvent {
    // Lifecycle events
    object START : PaymentProcessingEvent()
    object TRANSACTION_DONE : PaymentProcessingEvent()
    object CANCELLED : PaymentProcessingEvent()
    
    // Card interaction events
    object CARD_REACH_OR_INSERT : PaymentProcessingEvent()
    object CARD_INSERTED : PaymentProcessingEvent()
    object CARD_REMOVAL_REQUESTING : PaymentProcessingEvent()
    object CARD_REMOVAL_SUCCEEDED : PaymentProcessingEvent()
    object USE_CHIP : PaymentProcessingEvent()
    object USE_MAGNETIC_STRIPE : PaymentProcessingEvent()
    object SWIPE_CARD_REQUESTED : PaymentProcessingEvent()
    
    // PIN and authentication events
    object PIN_REQUESTED : PaymentProcessingEvent()
    object PIN_DIGIT_INPUT : PaymentProcessingEvent()
    object PIN_DIGIT_REMOVED : PaymentProcessingEvent()
    object PIN_OK : PaymentProcessingEvent()
    
    // Card validation events
    object CARD_BIN_REQUESTED : PaymentProcessingEvent()
    object CARD_BIN_OK : PaymentProcessingEvent()
    object CARD_HOLDER_REQUESTED : PaymentProcessingEvent()
    object CARD_HOLDER_OK : PaymentProcessingEvent()
    object CVV_REQUESTED : PaymentProcessingEvent()
    object CVV_OK : PaymentProcessingEvent()
    
    // Transaction processing events
    object TRANSACTION_PROCESSING : PaymentProcessingEvent()
    object AUTHORIZING : PaymentProcessingEvent()
    object APPROVAL_SUCCEEDED : PaymentProcessingEvent()
    object APPROVAL_DECLINED : PaymentProcessingEvent()
    object PARTIALLY_APPROVED : PaymentProcessingEvent()
    object APPROVED_UPDATE_TRACK_3 : PaymentProcessingEvent()
    object APPROVED_VIP : PaymentProcessingEvent()
    
    // Contactless events
    object CONTACTLESS_ON_DEVICE : PaymentProcessingEvent()
    object CONTACTLESS_ERROR : PaymentProcessingEvent()
    
    // System events
    object DOWNLOADING_TABLES : PaymentProcessingEvent()
    object SAVING_TABLES : PaymentProcessingEvent()
    object KEY_INSERTED : PaymentProcessingEvent()
    object ACTIVATION_SUCCEEDED : PaymentProcessingEvent()
    object SOLVING_PENDING_ISSUES : PaymentProcessingEvent()
    object REVERSING_TRANSACTION_WITH_ERROR : PaymentProcessingEvent()
    object SELECT_PAYMENT_METHOD : PaymentProcessingEvent()
    object SWITCH_INTERFACE : PaymentProcessingEvent()
    object REQUEST_IN_PROGRESS : PaymentProcessingEvent()
    
    // QR Code events
    data class QRCODE_SCAN(
        val qrCode: Bitmap,
        val timeoutMs: Long = 30000L
    ) : PaymentProcessingEvent()
    
    // Generic events
    object GENERIC_ERROR : PaymentProcessingEvent()
    object GENERIC_SUCCESS : PaymentProcessingEvent()
}
```

## Event Details

### Lifecycle Events

#### START
Emitted when payment processing begins.

```kotlin
object START : PaymentProcessingEvent()
```

**When emitted**: At the beginning of `processPayment()`
**UI Action**: Show "Payment started" message, initialize progress indicators

#### TRANSACTION_DONE
Emitted when payment processing is complete (success or failure).

```kotlin
object TRANSACTION_DONE : PaymentProcessingEvent()
```

**When emitted**: At the end of payment processing
**UI Action**: Hide progress indicators, show final result

#### CANCELLED
Emitted when payment processing is cancelled by user or system.

```kotlin
object CANCELLED : PaymentProcessingEvent()
```

**When emitted**: When `abort()` is called or user cancels
**UI Action**: Show cancellation message, reset UI state

### Card Interaction Events

#### CARD_REACH_OR_INSERT
Emitted when the system is ready for card interaction.

```kotlin
object CARD_REACH_OR_INSERT : PaymentProcessingEvent()
```

**When emitted**: After transaction initialization, waiting for card
**UI Action**: Show "Please insert or tap your card" message with card animation

#### PIN_REQUESTED
Emitted when PIN entry is required (debit cards).

```kotlin
object PIN_REQUESTED : PaymentProcessingEvent()
```

**When emitted**: During debit card processing when PIN is required
**UI Action**: Show "Please enter your PIN" message

#### PIN_DIGIT_INPUT
Emitted when user inputs a PIN digit.

```kotlin
object PIN_DIGIT_INPUT : PaymentProcessingEvent()
```

**When emitted**: Each time user presses a digit during PIN entry
**UI Action**: Update PIN display (show asterisks)

#### PIN_DIGIT_REMOVED
Emitted when user removes a PIN digit.

```kotlin
object PIN_DIGIT_REMOVED : PaymentProcessingEvent()
```

**When emitted**: When user presses backspace during PIN entry
**UI Action**: Remove last asterisk from PIN display

#### PIN_OK
Emitted when PIN is successfully validated.

```kotlin
object PIN_OK : PaymentProcessingEvent()
```

**When emitted**: After PIN validation succeeds
**UI Action**: Hide PIN entry interface, show processing state

### Transaction Processing Events

#### TRANSACTION_PROCESSING
Emitted when the actual transaction is being processed by the acquirer.

```kotlin
object TRANSACTION_PROCESSING : PaymentProcessingEvent()
```

**When emitted**: After card data is captured, during acquirer communication
**UI Action**: Show "Processing transaction..." with spinner/progress animation

#### APPROVAL_SUCCEEDED
Emitted when the transaction is approved by the acquirer.

```kotlin
object APPROVAL_SUCCEEDED : PaymentProcessingEvent()
```

**When emitted**: When acquirer approves the transaction
**UI Action**: Show success animation, "Payment approved" message

#### APPROVAL_DECLINED
Emitted when the transaction is declined by the acquirer.

```kotlin
object APPROVAL_DECLINED : PaymentProcessingEvent()
```

**When emitted**: When acquirer declines the transaction
**UI Action**: Show error animation, "Payment declined" message

### QR Code Events

#### QRCODE_SCAN
Emitted when QR code scanning is required (typically for PIX payments).

```kotlin
data class QRCODE_SCAN(
    val qrCode: Bitmap,
    val timeoutMs: Long = 30000L
) : PaymentProcessingEvent()
```

**When emitted**: During PIX payment processing when QR code needs to be displayed
**UI Action**: Show QR code dialog with the provided bitmap and countdown timer

### Error Events

#### GENERIC_ERROR
Emitted for general payment processing errors.

```kotlin
object GENERIC_ERROR : PaymentProcessingEvent()
```

**When emitted**: When an unspecified error occurs during processing
**UI Action**: Show generic error message

#### GENERIC_SUCCESS
Emitted for general payment processing success.

```kotlin
object GENERIC_SUCCESS : PaymentProcessingEvent()
```

**When emitted**: When payment completes successfully without specific success event
**UI Action**: Show generic success message

## Event Flow Examples

### Successful Credit Card Payment (Chip)
```
START
↓
CARD_REACH_OR_INSERT
↓
USE_CHIP
↓
CARD_INSERTED
↓
CARD_BIN_REQUESTED
↓
CARD_BIN_OK
↓
CARD_HOLDER_REQUESTED
↓
CARD_HOLDER_OK
↓
TRANSACTION_PROCESSING
↓
AUTHORIZING
↓
APPROVAL_SUCCEEDED
↓
CARD_REMOVAL_REQUESTING
↓
CARD_REMOVAL_SUCCEEDED
↓
TRANSACTION_DONE
```

### Successful Debit Card Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
USE_CHIP
↓
CARD_INSERTED
↓
CARD_BIN_REQUESTED
↓
CARD_BIN_OK
↓
PIN_REQUESTED
↓
PIN_DIGIT_INPUT (multiple times)
↓
PIN_OK
↓
TRANSACTION_PROCESSING
↓
AUTHORIZING
↓
APPROVAL_SUCCEEDED
↓
CARD_REMOVAL_REQUESTING
↓
CARD_REMOVAL_SUCCEEDED
↓
TRANSACTION_DONE
```

### PIX Payment with QR Code
```
START
↓
QRCODE_SCAN (with QR bitmap)
↓
TRANSACTION_PROCESSING
↓
APPROVAL_SUCCEEDED
↓
TRANSACTION_DONE
```

### Contactless Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
CONTACTLESS_ON_DEVICE
↓
TRANSACTION_PROCESSING
↓
AUTHORIZING
↓
APPROVAL_SUCCEEDED
↓
TRANSACTION_DONE
```

### Declined Payment
```
START
↓
CARD_REACH_OR_INSERT
↓
USE_CHIP
↓
CARD_INSERTED
↓
TRANSACTION_PROCESSING
↓
AUTHORIZING
↓
APPROVAL_DECLINED
↓
CARD_REMOVAL_REQUESTING
↓
CARD_REMOVAL_SUCCEEDED
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
                PaymentProcessingEvent.START -> {
                    updateUI("Payment started")
                    showProgressIndicator(true)
                }
                
                PaymentProcessingEvent.CARD_REACH_OR_INSERT -> {
                    updateUI("Please insert or tap your card")
                    showCardAnimation()
                }
                
                PaymentProcessingEvent.PIN_REQUESTED -> {
                    updateUI("Please enter your PIN")
                    showPinAnimation()
                }
                
                PaymentProcessingEvent.TRANSACTION_PROCESSING -> {
                    updateUI("Processing transaction...")
                    showProcessingAnimation()
                }
                
                PaymentProcessingEvent.APPROVAL_SUCCEEDED -> {
                    updateUI("Payment approved!")
                    showSuccessAnimation()
                    playSuccessSound()
                }
                
                PaymentProcessingEvent.APPROVAL_DECLINED -> {
                    updateUI("Payment declined")
                    showErrorAnimation()
                    playErrorSound()
                }
                
                PaymentProcessingEvent.PRINTING_RECEIPT -> {
                    updateUI("Printing receipt...")
                    showPrintingAnimation()
                }
                
                PaymentProcessingEvent.TRANSACTION_DONE -> {
                    showProgressIndicator(false)
                    // Transaction complete - show final state
                }
                
                PaymentProcessingEvent.CANCELLED -> {
                    updateUI("Payment cancelled")
                    showProgressIndicator(false)
                    resetUI()
                }
                
                PaymentProcessingEvent.GENERIC_ERROR -> {
                    updateUI("Payment error occurred")
                    showErrorAnimation()
                }
                
                PaymentProcessingEvent.GENERIC_SUCCESS -> {
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
    
    private fun handlePaymentEvent(event: PaymentProcessingEvent) {
        _uiState.value = when (event) {
            PaymentProcessingEvent.START -> _uiState.value.copy(
                status = "Payment started",
                isProcessing = true,
                showCardAnimation = false,
                showSuccessAnimation = false,
                showErrorAnimation = false
            )
            
            PaymentProcessingEvent.CARD_REACH_OR_INSERT -> _uiState.value.copy(
                status = "Please insert or tap your card",
                showCardAnimation = true,
                showPinAnimation = false,
                showProcessingAnimation = false
            )
            
            PaymentProcessingEvent.PIN_REQUESTED -> _uiState.value.copy(
                status = "Please enter your PIN",
                showCardAnimation = false,
                showPinAnimation = true
            )
            
            PaymentProcessingEvent.TRANSACTION_PROCESSING -> _uiState.value.copy(
                status = "Processing transaction...",
                showCardAnimation = false,
                showPinAnimation = false,
                showProcessingAnimation = true
            )
            
            PaymentProcessingEvent.APPROVAL_SUCCEEDED -> _uiState.value.copy(
                status = "Payment approved!",
                showProcessingAnimation = false,
                showSuccessAnimation = true
            )
            
            PaymentProcessingEvent.APPROVAL_DECLINED -> _uiState.value.copy(
                status = "Payment declined",
                showProcessingAnimation = false,
                showErrorAnimation = true
            )
            
            PaymentProcessingEvent.PRINTING_RECEIPT -> _uiState.value.copy(
                status = "Printing receipt...",
                showSuccessAnimation = false,
                showPrintingAnimation = true
            )
            
            PaymentProcessingEvent.TRANSACTION_DONE -> _uiState.value.copy(
                isProcessing = false,
                showPrintingAnimation = false
            )
            
            PaymentProcessingEvent.CANCELLED -> _uiState.value.copy(
                status = "Payment cancelled",
                isProcessing = false,
                showCardAnimation = false,
                showPinAnimation = false,
                showProcessingAnimation = false,
                showSuccessAnimation = false,
                showErrorAnimation = true
            )
            
            PaymentProcessingEvent.GENERIC_ERROR -> _uiState.value.copy(
                status = "Payment error occurred",
                showProcessingAnimation = false,
                showErrorAnimation = true
            )
            
            PaymentProcessingEvent.GENERIC_SUCCESS -> _uiState.value.copy(
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
override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
    _events.emit(PaymentProcessingEvent.START) // At the beginning
    
    try {
        _events.emit(PaymentProcessingEvent.CARD_REACH_OR_INSERT) // Before waiting for card
        
        val cardData = waitForCard()
        
        _events.emit(PaymentProcessingEvent.TRANSACTION_PROCESSING) // Before acquirer call
        
        val result = processWithAcquirer(cardData)
        
        if (result.isApproved) {
            _events.emit(PaymentProcessingEvent.APPROVAL_SUCCEEDED) // Immediately after approval
        } else {
            _events.emit(PaymentProcessingEvent.APPROVAL_DECLINED) // Immediately after decline
        }
        
        return result
        
    } finally {
        _events.emit(PaymentProcessingEvent.TRANSACTION_DONE) // Always at the end
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
    _events.emit(PaymentProcessingEvent.GENERIC_ERROR)
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

private fun handleEventImmediately(event: PaymentProcessingEvent) {
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
private fun emitEvent(event: PaymentProcessingEvent) {
    Log.d("PaymentProcessor", "Emitting event: $event")
    _events.emit(event)
    
    // Optional: Send to analytics
    analytics.trackPaymentEvent(event)
}
```
