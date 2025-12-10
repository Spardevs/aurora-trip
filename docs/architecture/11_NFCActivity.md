# NFCActivity

## Overview

`NFCActivity` handles NFC tag operations including customer authentication, tag formatting, cart operations, and balance management. It uses a queue-based system similar to payment processing.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/nfc/NFCActivity.kt
```

## Responsibilities

1. **NFC Queue** - Manage queue of NFC operations
2. **Tag Operations** - Read, write, format NFC tags
3. **Customer Auth** - Authenticate customers via NFC
4. **Cart Management** - Read/update cart on NFC tags
5. **Balance Operations** - Read/set/clear balance on tags

## Supported NFC Methods

| Method | Enum Value | Description |
|--------|------------|-------------|
| Customer Auth | `CUSTOMER_AUTH` | Authenticate customer |
| Customer Setup | `CUSTOMER_SETUP` | Setup new customer tag |
| Tag Format | `TAG_FORMAT` | Format/initialize tag |
| Cart Read | `CART_READ` | Read cart from tag |
| Cart Update | `CART_UPDATE` | Update cart on tag |
| Balance Read | `BALANCE_READ` | Read balance from tag |
| Balance Set | `BALANCE_SET` | Set balance on tag |
| Balance Clear | `BALANCE_CLEAR` | Clear balance from tag |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        NFCActivity                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    NFCViewModel                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                  Queue Manager                       │  │  │
│  │  │  [NFC Op 1] [NFC Op 2] [NFC Op 3] ...               │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Coordination Layer                        │  │
│  │  • NFCActivityCoordinator                                 │  │
│  │  • NFCDialogManager                                       │  │
│  │  • NFCEventHandler                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Acquirer SDK                            │  │
│  │  • AcquirerSdk.initialize()                               │  │
│  │  • NFCProvider                                            │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## ViewModel

### NFCViewModel

```kotlin
@HiltViewModel
class NFCViewModel @Inject constructor(
    private val nfcQueueManager: NFCQueueManager,
    private val nfcProcessor: NFCProcessor
) : ViewModel() {

    val queueState: StateFlow<NFCQueueState> = nfcQueueManager.queueState
    val processingState: StateFlow<NFCProcessingState> = nfcProcessor.state

    fun enqueueAuthOperation() {
        nfcQueueManager.enqueue(NFCOperation.CustomerAuth)
    }

    fun enqueueCartReadOperation() {
        nfcQueueManager.enqueue(NFCOperation.CartRead)
    }

    fun enqueueCartUpdateOperation(cart: CartOperation) {
        nfcQueueManager.enqueue(NFCOperation.CartUpdate(cart))
    }

    fun enqueueBalanceSetOperation(amount: Long) {
        nfcQueueManager.enqueue(NFCOperation.BalanceSet(amount))
    }

    fun startProcessing() {
        viewModelScope.launch {
            nfcQueueManager.processNext()
        }
    }

    fun cancelNFC(nfcId: String) {
        nfcQueueManager.cancel(nfcId)
    }

    fun abortNFC() {
        nfcProcessor.abort()
    }
}
```

## NFC Operations

### CartOperation

```kotlin
data class CartOperation(
    val action: CartAction,
    val productId: String?,
    val quantity: Int?,
    val price: Long?
)

enum class CartAction {
    SET, INCREMENT, DECREMENT, REMOVE, CLEAR
}
```

### BalanceOperation

```kotlin
data class BalanceOperation(
    val action: BalanceAction,
    val amount: Long?
)

enum class BalanceAction {
    READ, SET, CLEAR
}
```

## Coordination Components

### NFCActivityCoordinator

```kotlin
class NFCActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val nfcViewModel: NFCViewModel,
    private val dialogManager: NFCDialogManager,
    private val eventHandler: NFCEventHandler,
    private val queueView: NFCQueueView,
    // ... UI components
) {
    fun initialize() {
        observeQueueState()
        observeProcessingState()
        observeEvents()
    }
}
```

### NFCDialogManager

```kotlin
class NFCDialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val nfcViewModel: NFCViewModel
) {
    fun showProgressDialog()
    fun hideProgressDialog()
    fun showCartUpdateDialog(onConfirm: (CartOperation) -> Unit)
    fun showBalanceSetDialog(onConfirm: (Long) -> Unit)
}
```

## Custom Views

### NFCQueueView

```kotlin
class NFCQueueView : RecyclerView {
    var onNFCCanceled: ((String) -> Unit)? = null
    
    fun updateQueue(operations: List<NFCQueueItem>)
}
```

## Dynamic Button Generation

```kotlin
private fun generateNFCMethodButtons() {
    val container = findViewById<LinearLayout>(R.id.nfc_methods_container)
    val supportedMethods = SupportedNFCMethods.methods
    
    supportedMethods.forEachIndexed { index, method ->
        val button = Button(this).apply {
            text = getNFCMethodDisplayName(method)
            setOnClickListener {
                when (method) {
                    SystemNFCMethod.CUSTOMER_AUTH -> enqueueAuthOperation()
                    SystemNFCMethod.CART_READ -> enqueueCartReadOperation()
                    SystemNFCMethod.CART_UPDATE -> showCartUpdateDialog()
                    SystemNFCMethod.BALANCE_SET -> showBalanceSetDialog()
                    // ...
                }
            }
        }
        container.addView(button)
    }
}
```

## Tag Data Structure

NFC tags store data in MIFARE Classic sectors:

```
Sector 0: Manufacturer data (read-only)
Sector 1: Customer ID
Sector 2: Balance
Sector 3-15: Cart items
```

## Layout

```
res/layout/activity_nfc.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `nfc_methods_container` | LinearLayout | Dynamic NFC buttons |
| `nfc_queue_view` | NFCQueueView | Queue display |
| `btn_start_processing` | Button | Start processing queue |
| `clear_list` | View | Cancel all operations |

## Dialogs

### Progress Dialog
```
res/layout/dialog_nfc_progress.xml
```

### Cart Update Dialog
- Product selection
- Quantity input
- Action selection (add/remove/clear)

### Balance Set Dialog
- Amount input
- Confirm/cancel buttons

## See Also

- [ProductsListActivity](./08_ProductsListActivity.md)
- [PaymentProcessingActivity](./10_PaymentProcessingActivity.md)
