# PaymentProcessingActivity

## Overview

`PaymentProcessingActivity` handles payment processing for various payment methods including credit, debit, PIX, voucher, and cash. It uses a queue-based system for processing multiple payments.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/payment/PaymentProcessingActivity.kt
```

## Responsibilities

1. **Payment Queue** - Manage queue of pending payments
2. **Payment Processing** - Execute payments via acquirer SDK
3. **UI Feedback** - Show progress, QR codes, and results
4. **Multiple Methods** - Support various payment types

## Supported Payment Methods

| Method | Enum Value | Description |
|--------|------------|-------------|
| Credit | `CREDIT` | Credit card payment |
| Debit | `DEBIT` | Debit card payment |
| Voucher | `VOUCHER` | Meal/food voucher |
| PIX | `PIX` | PIX instant payment |
| Merchant PIX | `MERCHANT_PIX` | Merchant-generated PIX |
| Cash | `CASH` | Cash payment |
| Lightning Bitcoin | `LN_BITCOIN` | Bitcoin Lightning Network |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   PaymentProcessingActivity                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              PaymentProcessingViewModel                    │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                  Queue Manager                       │  │  │
│  │  │  [Payment 1] [Payment 2] [Payment 3] ...            │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Coordination Layer                        │  │
│  │  • PaymentActivityCoordinator                             │  │
│  │  • PaymentDialogManager                                   │  │
│  │  • PaymentEventHandler                                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Acquirer SDK                            │  │
│  │  • AcquirerSdk.initialize()                               │  │
│  │  • PaymentProvider                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## ViewModel

### PaymentProcessingViewModel

```kotlin
@HiltViewModel
class PaymentProcessingViewModel @Inject constructor(
    private val paymentQueueManager: PaymentQueueManager,
    private val paymentProcessor: PaymentProcessor
) : ViewModel() {

    val queueState: StateFlow<PaymentQueueState> = paymentQueueManager.queueState
    val processingState: StateFlow<PaymentProcessingState> = paymentProcessor.state

    fun enqueuePayment(amount: Long, method: SystemPaymentMethod) {
        paymentQueueManager.enqueue(PaymentRequest(amount, method))
    }

    fun startProcessing() {
        viewModelScope.launch {
            paymentQueueManager.processNext()
        }
    }

    fun cancelPayment(paymentId: String) {
        paymentQueueManager.cancel(paymentId)
    }

    fun abortPayment() {
        paymentProcessor.abort()
    }
}
```

## Coordination Components

### PaymentActivityCoordinator

Coordinates between ViewModel, UI, and event handling:

```kotlin
class PaymentActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val paymentViewModel: PaymentProcessingViewModel,
    private val dialogManager: PaymentDialogManager,
    private val eventHandler: PaymentEventHandler,
    private val queueView: PaymentProcessingQueueView,
    // ... UI components
) {
    fun initialize() {
        observeQueueState()
        observeProcessingState()
        observeEvents()
    }
}
```

### PaymentDialogManager

Manages payment-related dialogs:

```kotlin
class PaymentDialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val paymentViewModel: PaymentProcessingViewModel
) {
    fun showProgressDialog()
    fun hideProgressDialog()
    fun showQRCodeScanDialog(qrCode: String)
    fun dismissQRCodeDialog()
}
```

### PaymentEventHandler

Handles payment events and updates UI:

```kotlin
class PaymentEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView
) {
    fun handleEvent(event: ProcessingPaymentEvent) {
        when (event) {
            ProcessingPaymentEvent.CARD_INSERTED -> showMessage("Cartão inserido")
            ProcessingPaymentEvent.PROCESSING -> showMessage("Processando...")
            ProcessingPaymentEvent.QRCODE_SCAN -> showQRCode()
            // ...
        }
    }
}
```

## Custom Views

### PaymentProcessingQueueView

Displays the payment queue with status indicators:

```kotlin
class PaymentProcessingQueueView : RecyclerView {
    var onPaymentCanceled: ((String) -> Unit)? = null
    
    fun updateQueue(payments: List<PaymentQueueItem>)
}
```

### TimeoutCountdownView

Shows countdown timer for payment timeout:

```kotlin
class TimeoutCountdownView : TextView {
    fun startCountdown(seconds: Int)
    fun stopCountdown()
}
```

## Dynamic Button Generation

Payment method buttons are generated dynamically:

```kotlin
private fun generatePaymentMethodButtons() {
    val container = findViewById<LinearLayout>(R.id.payment_methods_container)
    val supportedMethods = SupportedPaymentMethods.methods
    
    supportedMethods.forEachIndexed { index, method ->
        val button = Button(this).apply {
            text = getPaymentMethodDisplayName(method)
            setOnClickListener {
                enqueuePayment(method)
            }
        }
        container.addView(button)
    }
}
```

## SDK Integration

### AcquirerSdk

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    AcquirerSdk.initialize(applicationContext)
    super.onCreate(savedInstanceState)
    // ...
}
```

The SDK is flavor-specific:
- **PagSeguro** - Uses PlugPag SDK
- **Stone** - Uses Stone SDK

## Layout

```
res/layout/activity_payment_processing.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `payment_methods_container` | LinearLayout | Dynamic payment buttons |
| `payment_queue_view` | PaymentProcessingQueueView | Queue display |
| `btn_start_processing` | Button | Start processing queue |
| `clear_list` | View | Cancel all payments |
| `checkbox_transactionless` | CheckBox | Test mode toggle |

## Dialogs

### Progress Dialog
```
res/layout/dialog_payment_progress.xml
```
- Progress bar
- Status text
- Cancel button
- Countdown timer

### QR Code Dialog
- QR code image
- Abort button
- Countdown timer

## See Also

- [ProductsListActivity](./08_ProductsListActivity.md)
- [Payment Methods System](../features/payment-methods-system.md)
