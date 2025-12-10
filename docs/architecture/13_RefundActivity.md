# RefundActivity

## Overview

`RefundActivity` handles refund processing for previously completed transactions. It requires transaction identifiers (ATK or Transaction ID) to process refunds.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/refund/RefundActivity.kt
```

## Responsibilities

1. **Refund Queue** - Manage queue of refund requests
2. **Transaction Lookup** - Identify transactions by ATK or TX ID
3. **Refund Execution** - Process refunds via acquirer SDK
4. **Status Feedback** - Show refund progress and results

## Supported Refund Methods

| Method | Enum Value | Description |
|--------|------------|-------------|
| Acquirer | `ACQUIRER` | Standard acquirer refund |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       RefundActivity                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   RefundViewModel                          │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                  Queue Manager                       │  │  │
│  │  │  [Refund 1] [Refund 2] [Refund 3] ...               │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Coordination Layer                        │  │
│  │  • RefundActivityCoordinator                              │  │
│  │  • RefundDialogManager                                    │  │
│  │  • RefundEventHandler                                     │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Acquirer SDK                            │  │
│  │  • AcquirerSdk.initialize()                               │  │
│  │  • RefundProvider                                         │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Input Fields

| Field | Purpose |
|-------|---------|
| ATK | Acquirer Transaction Key |
| TX ID | Transaction ID |
| Is QR Code | Flag for QR code transactions |

## ViewModel

### RefundViewModel

```kotlin
@HiltViewModel
class RefundViewModel @Inject constructor(
    private val refundQueueManager: RefundQueueManager,
    private val refundProcessor: RefundProcessor
) : ViewModel() {

    val queueState: StateFlow<RefundQueueState> = refundQueueManager.queueState
    val processingState: StateFlow<RefundProcessingState> = refundProcessor.state

    fun enqueueRefund(
        atk: String,
        txId: String?,
        isQRCode: Boolean?,
        processorType: RefundProcessorType
    ) {
        refundQueueManager.enqueue(RefundRequest(atk, txId, isQRCode, processorType))
    }

    fun startProcessing() {
        viewModelScope.launch {
            refundQueueManager.processNext()
        }
    }

    fun cancelRefund(refundId: String) {
        refundQueueManager.cancel(refundId)
    }

    fun abortRefund() {
        refundProcessor.abort()
    }

    fun cancelAllRefunds() {
        refundQueueManager.cancelAll()
    }
}
```

## Coordination Components

### RefundActivityCoordinator

```kotlin
class RefundActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val refundViewModel: RefundViewModel,
    private val dialogManager: RefundDialogManager,
    private val eventHandler: RefundEventHandler,
    private val queueView: RefundQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val dialogRefundMethodTextView: TextView,
    private val showProgressDialog: () -> Unit,
    private val hideProgressDialog: () -> Unit
) {
    fun initialize() {
        observeQueueState()
        observeProcessingState()
        observeEvents()
    }
}
```

### RefundEventHandler

```kotlin
class RefundEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView
) {
    fun handleEvent(event: RefundEvent) {
        when (event) {
            RefundEvent.REFUND_STARTED -> showMessage("Processando estorno...")
            RefundEvent.REFUND_COMPLETE -> showMessage("Estorno concluído")
            RefundEvent.REFUND_ERROR -> showMessage("Erro no estorno")
            // ...
        }
    }
}
```

## Custom Views

### RefundQueueView

```kotlin
class RefundQueueView : RecyclerView {
    var onRefundCanceled: ((String) -> Unit)? = null
    
    fun updateQueue(refunds: List<RefundQueueItem>)
}
```

## Implementation

```kotlin
@AndroidEntryPoint
class RefundActivity : AppCompatActivity() {
    
    private val refundViewModel: RefundViewModel by viewModels()
    
    private lateinit var editTextAtk: TextInputEditText
    private lateinit var editTextTxId: TextInputEditText
    private lateinit var checkboxIsQRCode: AppCompatCheckBox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        AcquirerSdk.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refund_processing)
        
        setupViews()
        setupButtons()
        initializeComponents()
        coordinator.initialize()
    }
    
    private fun setupViews() {
        queueView = findViewById(R.id.refund_queue_view)
        editTextAtk = findViewById(R.id.edit_text_atk)
        editTextTxId = findViewById(R.id.edit_text_tx_id)
        checkboxIsQRCode = findViewById(R.id.checkbox_is_qrcode)
        
        queueView.onRefundCanceled = { refundId ->
            refundViewModel.cancelRefund(refundId)
        }
        createProgressDialog()
    }
    
    private fun enqueueRefund(
        atk: String,
        txId: String?,
        isQRCode: Boolean?,
        processorType: SystemRefundMethod
    ) {
        val processorType = RefundProcessorType.entries.find { it.name == processorType.name }
            ?: throw IllegalArgumentException("Unsupported refund method")
            
        val refundData = RefundUIUtils.createRefundData(
            atk = atk,
            txId = txId,
            isQRCode = isQRCode,
            processorType = processorType
        )

        refundViewModel.enqueueRefund(
            atk = refundData.atk,
            txId = refundData.txId,
            isQRCode = refundData.isQRCode,
            processorType = refundData.processorType
        )
    }
}
```

## Layout

```
res/layout/activity_refund_processing.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `refund_methods_container` | LinearLayout | Dynamic refund buttons |
| `refund_queue_view` | RefundQueueView | Queue display |
| `edit_text_atk` | TextInputEditText | ATK input |
| `edit_text_tx_id` | TextInputEditText | Transaction ID input |
| `checkbox_is_qrcode` | AppCompatCheckBox | QR code flag |
| `btn_start_processing` | Button | Start processing queue |
| `clear_list` | View | Cancel all refunds |
| `text_refund_queue_title` | TextView | Queue title |

## Dialogs

### Progress Dialog
```
res/layout/dialog_refund_progress.xml
```
- Progress bar
- Status text
- Refund method indicator
- Cancel button

## Refund Data

### RefundUIUtils

```kotlin
object RefundUIUtils {
    fun createRefundData(
        atk: String,
        txId: String?,
        isQRCode: Boolean?,
        processorType: RefundProcessorType
    ): RefundData {
        return RefundData(
            atk = atk,
            txId = txId,
            isQRCode = isQRCode,
            processorType = processorType
        )
    }
}
```

## See Also

- [ProductsListActivity](./08_ProductsListActivity.md)
- [PaymentProcessingActivity](./10_PaymentProcessingActivity.md)
