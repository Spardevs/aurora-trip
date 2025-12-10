# PrintingActivity

## Overview

`PrintingActivity` handles receipt printing operations. It supports multiple printer types and uses a queue-based system for processing print jobs.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/printing/PrintingActivity.kt
```

## Responsibilities

1. **Print Queue** - Manage queue of print jobs
2. **Printer Selection** - Support multiple printer types
3. **Print Execution** - Send data to printer via SDK
4. **Status Feedback** - Show print progress and errors

## Supported Printing Methods

| Method | Enum Value | Description |
|--------|------------|-------------|
| Acquirer | `ACQUIRER` | Built-in POS printer |
| MP 4200 HS | `MP_4200_HS` | External Bluetooth printer |
| MPT II | `MPT_II` | External Bluetooth printer |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      PrintingActivity                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  PrintingViewModel                         │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                  Queue Manager                       │  │  │
│  │  │  [Print 1] [Print 2] [Print 3] ...                  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Coordination Layer                        │  │
│  │  • PrintingActivityCoordinator                            │  │
│  │  • PrintingDialogManager                                  │  │
│  │  • PrintingEventHandler                                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Acquirer SDK                            │  │
│  │  • AcquirerSdk.initialize()                               │  │
│  │  • PrintingProvider                                       │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## ViewModel

### PrintingViewModel

```kotlin
@HiltViewModel
class PrintingViewModel @Inject constructor(
    private val printingQueueManager: PrintingQueueManager,
    private val printingProcessor: PrintingProcessor
) : ViewModel() {

    val queueState: StateFlow<PrintingQueueState> = printingQueueManager.queueState
    val processingState: StateFlow<PrintingProcessingState> = printingProcessor.state

    fun enqueuePrinting(filePath: String, processorType: PrintingProcessorType) {
        printingQueueManager.enqueue(PrintingRequest(filePath, processorType))
    }

    fun startProcessing() {
        viewModelScope.launch {
            printingQueueManager.processNext()
        }
    }

    fun cancelPrinting(printingId: String) {
        printingQueueManager.cancel(printingId)
    }

    fun abortPrinting() {
        printingProcessor.abort()
    }

    fun cancelAllPrintings() {
        printingQueueManager.cancelAll()
    }
}
```

## Coordination Components

### PrintingActivityCoordinator

```kotlin
class PrintingActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val printingViewModel: PrintingViewModel,
    private val dialogManager: PrintingDialogManager,
    private val eventHandler: PrintingEventHandler,
    private val queueView: PrintingQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val dialogPrintingMethodTextView: TextView,
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

### PrintingEventHandler

```kotlin
class PrintingEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView
) {
    fun handleEvent(event: PrintingEvent) {
        when (event) {
            PrintingEvent.PRINTING_STARTED -> showMessage("Imprimindo...")
            PrintingEvent.PRINTING_COMPLETE -> showMessage("Impressão concluída")
            PrintingEvent.PRINTER_ERROR -> showMessage("Erro na impressora")
            // ...
        }
    }
}
```

## Custom Views

### PrintingQueueView

```kotlin
class PrintingQueueView : RecyclerView {
    var onPrintingCanceled: ((String) -> Unit)? = null
    
    fun updateQueue(printJobs: List<PrintingQueueItem>)
}
```

## Dynamic Button Generation

```kotlin
private fun generatePrintingMethodButtons() {
    val container = findViewById<LinearLayout>(R.id.printing_methods_container)
    val supportedMethods = SupportedPrintingMethods.methods
    
    container.removeAllViews()
    
    val buttonsPerRow = 2
    var currentRow: LinearLayout? = null
    
    supportedMethods.forEachIndexed { index, method ->
        if (index % buttonsPerRow == 0) {
            currentRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                // ...
            }
            container.addView(currentRow)
        }
        
        val button = Button(this).apply {
            text = getPrintingMethodDisplayName(method)
            setOnClickListener {
                enqueuePrinting(method)
            }
        }
        
        currentRow?.addView(button)
    }
}

private fun getPrintingMethodDisplayName(method: SystemPrintingMethod): String {
    return when (method) {
        SystemPrintingMethod.ACQUIRER -> getString(R.string.enqueue_acquirer_printing)
        SystemPrintingMethod.MP_4200_HS -> getString(R.string.enqueue_mp4200HS_printing)
        SystemPrintingMethod.MPT_II -> getString(R.string.enqueue_mptII_printing)
    }
}
```

## Print Data

### PrintingUIUtils

```kotlin
object PrintingUIUtils {
    fun createPrintingData(
        filePath: String,
        processorType: PrintingProcessorType
    ): PrintingData {
        return PrintingData(
            filePath = filePath,
            processorType = processorType
        )
    }
}
```

## Layout

```
res/layout/activity_printing_processing.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `printing_methods_container` | LinearLayout | Dynamic printer buttons |
| `printing_queue_view` | PrintingQueueView | Queue display |
| `btn_start_processing` | Button | Start processing queue |
| `clear_list` | View | Cancel all print jobs |
| `text_printing_queue_title` | TextView | Queue title |

## Dialogs

### Progress Dialog
```
res/layout/dialog_printing_progress.xml
```
- Progress bar
- Status text
- Printer type indicator
- Cancel button

## See Also

- [ProductsListActivity](./08_ProductsListActivity.md)
- [PaymentProcessingActivity](./10_PaymentProcessingActivity.md)
