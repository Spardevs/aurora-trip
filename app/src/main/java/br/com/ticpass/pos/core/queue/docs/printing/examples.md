# Print Queue Examples

Complete usage examples for implementing print job processing with the queue system.

## Basic Print Job Processing

### ViewModel Implementation
```kotlin
class PrintViewModel : ViewModel() {
    private val printQueue = PrintQueueFactory().createPrintQueue(
        storage = PrintStorage(dao),
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode = ProcessorStartMode.MANUAL,
        scope = viewModelScope
    )
    
    private val _uiState = MutableStateFlow(PrintUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        observeProcessingState()
        observePrintEvents()
    }
    
    fun addPrintJob(content: String, printerType: PrinterType, copies: Int = 1) {
        val printJob = PrintQueueItem(
            id = UUID.randomUUID().toString(),
            content = content,
            printerType = printerType,
            copies = copies,
            status = QueueItemStatus.PENDING
        )
        
        viewModelScope.launch {
            printQueue.enqueue(printJob)
        }
    }
    
    fun startPrinting() {
        viewModelScope.launch {
            printQueue.startProcessing()
        }
    }
    
    fun abortPrinting() {
        viewModelScope.launch {
            printQueue.abort()
        }
    }
    
    private fun observeProcessingState() {
        viewModelScope.launch {
            printQueue.processingState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    processingState = when (state) {
                        is ProcessingState.QueueIdle -> "Ready to print"
                        is ProcessingState.ItemProcessing -> "Printing job ${state.item.id}"
                        is ProcessingState.ItemDone -> "Print job completed"
                        is ProcessingState.ItemFailed -> "Print failed: ${state.error}"
                        is ProcessingState.QueueDone -> "All print jobs completed"
                        else -> "Unknown state"
                    }
                )
            }
        }
    }
    
    private fun observePrintEvents() {
        viewModelScope.launch {
            printQueue.processorEvents.collect { event ->
                handlePrintEvent(event)
            }
        }
    }
}
```

### Event Handling
```kotlin
private fun handlePrintEvent(event: PrintingEvent) {
    _uiState.value = when (event) {
        PrintingEvent.PRINT_STARTED -> _uiState.value.copy(
            statusMessage = "Printing started",
            isPrinting = true,
            showPrintingAnimation = true
        )
        
        PrintingEvent.PRINT_COMPLETED -> _uiState.value.copy(
            statusMessage = "Print completed",
            isPrinting = false,
            showPrintingAnimation = false,
            showSuccessAnimation = true
        )
        
        PrintingEvent.PRINTER_ERROR -> _uiState.value.copy(
            statusMessage = "Printer error",
            isPrinting = false,
            showPrintingAnimation = false,
            showErrorAnimation = true
        )
        
        PrintingEvent.PAPER_OUT -> _uiState.value.copy(
            statusMessage = "Paper out - please refill",
            isPrinting = false,
            showPrintingAnimation = false,
            showPaperOutAnimation = true
        )
        
        else -> _uiState.value
    }
}
```

## UI State Management

### Data Classes
```kotlin
data class PrintUiState(
    val statusMessage: String = "",
    val processingState: String = "",
    val isPrinting: Boolean = false,
    val showPrintingAnimation: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val showErrorAnimation: Boolean = false,
    val showPaperOutAnimation: Boolean = false,
    val queueItems: List<PrintQueueItem> = emptyList()
)

data class PrintQueueItem(
    override val id: String,
    override val priority: Int = 0,
    override var status: QueueItemStatus = QueueItemStatus.PENDING,
    val content: String,
    val printerType: PrinterType,
    val copies: Int = 1
) : QueueItem

enum class PrinterType {
    THERMAL,
    INKJET,
    LASER
}
```

## Compose UI Implementation

### Main Print Screen
```kotlin
@Composable
fun PrintScreen(
    viewModel: PrintViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = uiState.processingState,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Animation section
        PrintAnimationContainer(uiState = uiState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Queue section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Print Queue",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                LazyColumn {
                    items(uiState.queueItems) { item ->
                        PrintQueueItemRow(item = item)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    viewModel.addPrintJob("Receipt content", PrinterType.THERMAL) 
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isPrinting
            ) {
                Text("Add Receipt")
            }
            
            Button(
                onClick = { 
                    viewModel.addPrintJob("Report content", PrinterType.INKJET) 
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isPrinting
            ) {
                Text("Add Report")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.startPrinting() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isPrinting && uiState.queueItems.isNotEmpty()
            ) {
                Text("Start Printing")
            }
            
            Button(
                onClick = { viewModel.abortPrinting() },
                modifier = Modifier.weight(1f),
                enabled = uiState.isPrinting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Abort")
            }
        }
    }
}
```

### Animation Components
```kotlin
@Composable
fun PrintAnimationContainer(uiState: PrintUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.showPrintingAnimation -> {
                PrintingAnimation()
            }
            uiState.showSuccessAnimation -> {
                SuccessAnimation()
            }
            uiState.showErrorAnimation -> {
                ErrorAnimation()
            }
            uiState.showPaperOutAnimation -> {
                PaperOutAnimation()
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Printer",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PrintingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Print,
            contentDescription = "Printing",
            modifier = Modifier
                .size(48.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Printing...",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PaperOutAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Paper Out",
            modifier = Modifier
                .size(48.dp)
                .alpha(alpha),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Paper Out",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
```

### Queue Item Component
```kotlin
@Composable
fun PrintQueueItemRow(item: PrintQueueItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (item.status) {
                            QueueItemStatus.PENDING -> MaterialTheme.colorScheme.outline
                            QueueItemStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                            QueueItemStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            QueueItemStatus.FAILED -> MaterialTheme.colorScheme.error
                            QueueItemStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Print Job ${item.id.take(8)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.printerType} • ${item.copies} ${if (item.copies == 1) "copy" else "copies"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status text
            Text(
                text = item.status.name,
                style = MaterialTheme.typography.bodySmall,
                color = when (item.status) {
                    QueueItemStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                    QueueItemStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    QueueItemStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
```

## Advanced Examples

### Batch Printing
```kotlin
fun addBatchPrintJobs(receipts: List<String>) {
    viewModelScope.launch {
        receipts.forEachIndexed { index, content ->
            val printJob = PrintQueueItem(
                id = "batch_${System.currentTimeMillis()}_$index",
                content = content,
                printerType = PrinterType.THERMAL,
                copies = 1,
                priority = index // Lower index = higher priority
            )
            printQueue.enqueue(printJob)
        }
        
        // Start processing automatically for batch jobs
        printQueue.startProcessing()
    }
}
```

### Print with Confirmation
```kotlin
class PrintProcessor : QueueProcessor<PrintQueueItem, PrintingEvent> {
    
    override suspend fun process(item: PrintQueueItem): ProcessingResult {
        try {
            _events.emit(PrintingEvent.PRINT_STARTED)
            
            // Check printer status
            if (!isPrinterReady()) {
                return ProcessingResult.Error(ProcessingErrorEvent.PRINTER_NOT_READY)
            }
            
            // Print the job
            val success = printJob(item)
            
            if (success) {
                _events.emit(PrintingEvent.PRINT_COMPLETED)
                return ProcessingResult.Success("", item.id)
            } else {
                _events.emit(PrintingEvent.PRINTER_ERROR)
                return ProcessingResult.Error(ProcessingErrorEvent.PRINTER_ERROR)
            }
            
        } catch (e: PaperOutException) {
            _events.emit(PrintingEvent.PAPER_OUT)
            return ProcessingResult.Error(ProcessingErrorEvent.PRINTER_OUT_OF_PAPER)
        } catch (e: Exception) {
            _events.emit(PrintingEvent.PRINTER_ERROR)
            return ProcessingResult.Error(ProcessingErrorEvent.PRINTER_ERROR)
        }
    }
    
    private suspend fun printJob(item: PrintQueueItem): Boolean {
        repeat(item.copies) { copy ->
            val success = when (item.printerType) {
                PrinterType.THERMAL -> printThermal(item.content)
                PrinterType.INKJET -> printInkjet(item.content)
                PrinterType.LASER -> printLaser(item.content)
            }
            
            if (!success) return false
            
            // Small delay between copies
            if (copy < item.copies - 1) {
                delay(500)
            }
        }
        return true
    }
}
```

## Testing Examples

### Unit Tests
```kotlin
class PrintViewModelTest {
    
    @Test
    fun `addPrintJob should enqueue print item`() = runTest {
        // Given
        val viewModel = PrintViewModel()
        
        // When
        viewModel.addPrintJob("Test content", PrinterType.THERMAL)
        
        // Then
        verify(printQueue).enqueue(any<PrintQueueItem>())
    }
    
    @Test
    fun `print events should update UI state correctly`() = runTest {
        // Given
        val viewModel = PrintViewModel()
        
        // When
        printEventFlow.emit(PrintingEvent.PRINT_STARTED)
        
        // Then
        assertEquals("Printing started", viewModel.uiState.value.statusMessage)
        assertTrue(viewModel.uiState.value.isPrinting)
        assertTrue(viewModel.uiState.value.showPrintingAnimation)
    }
}
```

This comprehensive example demonstrates how to implement a complete print job processing system using the queue management framework with proper state management, event handling, and UI feedback.
