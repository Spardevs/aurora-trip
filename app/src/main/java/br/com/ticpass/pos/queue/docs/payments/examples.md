# Payment Queue Examples

Complete usage examples for implementing payment processing with the queue system.

## Basic Payment Processing

### ViewModel Implementation
```kotlin
class PaymentViewModel : ViewModel() {
    private val paymentQueue = PaymentProcessingQueueFactory().createDynamicPaymentQueue(
        storage = PaymentProcessingStorage(dao),
        persistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode = ProcessorStartMode.IMMEDIATE,
        scope = viewModelScope
    )
    
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        observeProcessingState()
        observePaymentEvents()
        observeQueueInputRequests()
        observeUserInputRequests()
    }
    
    fun processPayment(amount: Double, paymentMethod: PaymentMethod) {
        val paymentItem = PaymentProcessingQueueItem(
            id = UUID.randomUUID().toString(),
            amount = amount,
            commission = calculateCommission(amount),
            paymentMethod = paymentMethod,
            status = QueueItemStatus.PENDING
        )
        
        viewModelScope.launch {
            paymentQueue.enqueue(paymentItem)
        }
    }
    
    fun abortPayment() {
        viewModelScope.launch {
            paymentQueue.abort()
        }
    }
    
    private fun observeProcessingState() {
        viewModelScope.launch {
            paymentQueue.processingState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    processingState = when (state) {
                        is ProcessingState.QueueIdle -> "Ready to process"
                        is ProcessingState.ItemProcessing -> "Processing payment ${state.item.id}"
                        is ProcessingState.ItemDone -> "Payment completed"
                        is ProcessingState.ItemFailed -> "Payment failed: ${state.error}"
                        is ProcessingState.QueueDone -> "All payments completed"
                        else -> "Unknown state"
                    }
                )
            }
        }
    }
    
    private fun observePaymentEvents() {
        viewModelScope.launch {
            paymentQueue.processorEvents.collect { event ->
                handlePaymentEvent(event)
            }
        }
    }
    
    private fun observeQueueInputRequests() {
        viewModelScope.launch {
            paymentQueue.queueInputRequests.collect { request ->
                handleQueueInputRequest(request)
            }
        }
    }
    
    private fun observeUserInputRequests() {
        viewModelScope.launch {
            paymentQueue.processor.userInputRequests.collect { request ->
                handleUserInputRequest(request)
            }
        }
    }
}
```

### Event Handling
```kotlin
private fun handlePaymentEvent(event: PaymentProcessingEvent) {
    _uiState.value = when (event) {
        PaymentProcessingEvent.START -> _uiState.value.copy(
            statusMessage = "Payment started",
            isProcessing = true,
            showCardAnimation = false
        )
        
        PaymentProcessingEvent.CARD_REACH_OR_INSERT -> _uiState.value.copy(
            statusMessage = "Please insert or tap your card",
            showCardAnimation = true
        )
        
        PaymentProcessingEvent.PIN_REQUESTED -> _uiState.value.copy(
            statusMessage = "Please enter your PIN",
            showCardAnimation = false,
            showPinAnimation = true
        )
        
        PaymentProcessingEvent.TRANSACTION_PROCESSING -> _uiState.value.copy(
            statusMessage = "Processing transaction...",
            showCardAnimation = false,
            showPinAnimation = false,
            showProcessingAnimation = true
        )
        
        PaymentProcessingEvent.APPROVAL_SUCCEEDED -> _uiState.value.copy(
            statusMessage = "Payment approved!",
            showProcessingAnimation = false,
            showSuccessAnimation = true
        )
        
        PaymentProcessingEvent.APPROVAL_DECLINED -> _uiState.value.copy(
            statusMessage = "Payment declined",
            showProcessingAnimation = false,
            showErrorAnimation = true
        )
        
        PaymentProcessingEvent.PRINTING_RECEIPT -> _uiState.value.copy(
            statusMessage = "Printing receipt...",
            showPrintingAnimation = true
        )
        
        PaymentProcessingEvent.TRANSACTION_DONE -> _uiState.value.copy(
            isProcessing = false,
            showPrintingAnimation = false
        )
        
        PaymentProcessingEvent.CANCELLED -> _uiState.value.copy(
            statusMessage = "Payment cancelled",
            isProcessing = false,
            showCardAnimation = false,
            showPinAnimation = false,
            showProcessingAnimation = false,
            showErrorAnimation = true
        )
        
        else -> _uiState.value
    }
}
```

### Queue Input Request Handling
```kotlin
private fun handleQueueInputRequest(request: QueueInputRequest) {
    when (request) {
        is QueueInputRequest.ERROR_RETRY_OR_SKIP -> {
            _uiState.value = _uiState.value.copy(
                showErrorDialog = true,
                errorDialogData = ErrorDialogData(
                    error = request.error,
                    onRetry = {
                        viewModelScope.launch {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorRetry(request.id)
                            )
                        }
                        dismissErrorDialog()
                    },
                    onSkip = {
                        viewModelScope.launch {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorSkip(request.id)
                            )
                        }
                        dismissErrorDialog()
                    },
                    onAbort = {
                        viewModelScope.launch {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorAbort(request.id)
                            )
                        }
                        dismissErrorDialog()
                    },
                    onAbortAll = {
                        viewModelScope.launch {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.onErrorAbortAll(request.id)
                            )
                        }
                        dismissErrorDialog()
                    }
                )
            )
        }
        
        is QueueInputRequest.CONFIRM_NEXT_PROCESSOR -> {
            _uiState.value = _uiState.value.copy(
                showConfirmationDialog = true,
                confirmationDialogData = ConfirmationDialogData(
                    message = "Process payment ${request.currentItemIndex + 1} of ${request.totalItems}?",
                    onConfirm = {
                        viewModelScope.launch {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.proceed(request.id)
                            )
                        }
                        dismissConfirmationDialog()
                    },
                    onCancel = {
                        viewModelScope.launch {
                            paymentQueue.provideQueueInput(
                                QueueInputResponse.skip(request.id)
                            )
                        }
                        dismissConfirmationDialog()
                    }
                )
            )
        }
    }
}
```

### User Input Request Handling
```kotlin
private fun handleUserInputRequest(request: UserInputRequest) {
    when (request) {
        is UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING -> {
            _uiState.value = _uiState.value.copy(
                showReceiptDialog = true,
                receiptDialogData = ReceiptDialogData(
                    onConfirm = {
                        viewModelScope.launch {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, true)
                            )
                        }
                        dismissReceiptDialog()
                    },
                    onCancel = {
                        viewModelScope.launch {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, false)
                            )
                        }
                        dismissReceiptDialog()
                    }
                )
            )
        }
        
        is UserInputRequest.CONFIRM_MERCHANT_PIX_KEY -> {
            _uiState.value = _uiState.value.copy(
                showPixKeyDialog = true,
                pixKeyDialogData = PixKeyDialogData(
                    onConfirm = { pixKey ->
                        viewModelScope.launch {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, pixKey)
                            )
                        }
                        dismissPixKeyDialog()
                    },
                    onCancel = {
                        viewModelScope.launch {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, null, true)
                            )
                        }
                        dismissPixKeyDialog()
                    }
                )
            )
        }
        
        is UserInputRequest.MERCHANT_PIX_SCANNING -> {
            _uiState.value = _uiState.value.copy(
                showPixScanDialog = true,
                pixScanDialogData = PixScanDialogData(
                    pixCode = request.pixCode,
                    onScanned = { result ->
                        viewModelScope.launch {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, result)
                            )
                        }
                        dismissPixScanDialog()
                    },
                    onCancel = {
                        viewModelScope.launch {
                            paymentQueue.processor.provideUserInput(
                                UserInputResponse(request.id, null, true)
                            )
                        }
                        dismissPixScanDialog()
                    }
                )
            )
        }
    }
}
```

## UI State Management

### Data Classes
```kotlin
data class PaymentUiState(
    val statusMessage: String = "",
    val processingState: String = "",
    val isProcessing: Boolean = false,
    val showCardAnimation: Boolean = false,
    val showPinAnimation: Boolean = false,
    val showProcessingAnimation: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val showErrorAnimation: Boolean = false,
    val showPrintingAnimation: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorDialogData: ErrorDialogData? = null,
    val showConfirmationDialog: Boolean = false,
    val confirmationDialogData: ConfirmationDialogData? = null,
    val showReceiptDialog: Boolean = false,
    val receiptDialogData: ReceiptDialogData? = null,
    val showPixKeyDialog: Boolean = false,
    val pixKeyDialogData: PixKeyDialogData? = null,
    val showPixScanDialog: Boolean = false,
    val pixScanDialogData: PixScanDialogData? = null
)

data class ErrorDialogData(
    val error: ProcessingErrorEvent,
    val onRetry: () -> Unit,
    val onSkip: () -> Unit,
    val onAbort: () -> Unit,
    val onAbortAll: () -> Unit
)

data class ConfirmationDialogData(
    val message: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

data class ReceiptDialogData(
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

data class PixKeyDialogData(
    val onConfirm: (String) -> Unit,
    val onCancel: () -> Unit
)

data class PixScanDialogData(
    val pixCode: String,
    val onScanned: (Boolean) -> Unit,
    val onCancel: () -> Unit
)
```

## Compose UI Implementation

### Main Payment Screen
```kotlin
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status message
        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Animations
        AnimationContainer(uiState = uiState)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Processing state
        if (uiState.isProcessing) {
            CircularProgressIndicator()
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { 
                    viewModel.processPayment(100.0, PaymentMethod.CREDIT_CARD) 
                },
                enabled = !uiState.isProcessing
            ) {
                Text("Credit Card")
            }
            
            Button(
                onClick = { 
                    viewModel.processPayment(100.0, PaymentMethod.PIX) 
                },
                enabled = !uiState.isProcessing
            ) {
                Text("PIX")
            }
            
            Button(
                onClick = { 
                    viewModel.processPayment(100.0, PaymentMethod.CASH) 
                },
                enabled = !uiState.isProcessing
            ) {
                Text("Cash")
            }
        }
        
        if (uiState.isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.abortPayment() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Abort Payment")
            }
        }
    }
    
    // Dialogs
    PaymentDialogs(
        uiState = uiState,
        viewModel = viewModel
    )
}
```

### Animation Container
```kotlin
@Composable
fun AnimationContainer(uiState: PaymentUiState) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.showCardAnimation -> {
                CardAnimation()
            }
            uiState.showPinAnimation -> {
                PinAnimation()
            }
            uiState.showProcessingAnimation -> {
                ProcessingAnimation()
            }
            uiState.showSuccessAnimation -> {
                SuccessAnimation()
            }
            uiState.showErrorAnimation -> {
                ErrorAnimation()
            }
            uiState.showPrintingAnimation -> {
                PrintingAnimation()
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = "Payment",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### Dialog Components
```kotlin
@Composable
fun PaymentDialogs(
    uiState: PaymentUiState,
    viewModel: PaymentViewModel
) {
    // Error dialog
    if (uiState.showErrorDialog && uiState.errorDialogData != null) {
        ErrorDialog(
            error = uiState.errorDialogData.error,
            onRetry = uiState.errorDialogData.onRetry,
            onSkip = uiState.errorDialogData.onSkip,
            onAbort = uiState.errorDialogData.onAbort,
            onAbortAll = uiState.errorDialogData.onAbortAll
        )
    }
    
    // Confirmation dialog
    if (uiState.showConfirmationDialog && uiState.confirmationDialogData != null) {
        ConfirmationDialog(
            message = uiState.confirmationDialogData.message,
            onConfirm = uiState.confirmationDialogData.onConfirm,
            onCancel = uiState.confirmationDialogData.onCancel
        )
    }
    
    // Receipt dialog
    if (uiState.showReceiptDialog && uiState.receiptDialogData != null) {
        ReceiptDialog(
            onConfirm = uiState.receiptDialogData.onConfirm,
            onCancel = uiState.receiptDialogData.onCancel
        )
    }
    
    // PIX key dialog
    if (uiState.showPixKeyDialog && uiState.pixKeyDialogData != null) {
        PixKeyDialog(
            onConfirm = uiState.pixKeyDialogData.onConfirm,
            onCancel = uiState.pixKeyDialogData.onCancel
        )
    }
    
    // PIX scan dialog
    if (uiState.showPixScanDialog && uiState.pixScanDialogData != null) {
        PixScanDialog(
            pixCode = uiState.pixScanDialogData.pixCode,
            onScanned = uiState.pixScanDialogData.onScanned,
            onCancel = uiState.pixScanDialogData.onCancel
        )
    }
}
```

## Testing Examples

### Unit Tests
```kotlin
class PaymentViewModelTest {
    
    @Test
    fun `processPayment should enqueue payment item`() = runTest {
        // Given
        val viewModel = PaymentViewModel()
        
        // When
        viewModel.processPayment(100.0, PaymentMethod.CREDIT_CARD)
        
        // Then
        verify(paymentQueue).enqueue(any<PaymentProcessingQueueItem>())
    }
    
    @Test
    fun `payment events should update UI state correctly`() = runTest {
        // Given
        val viewModel = PaymentViewModel()
        
        // When
        paymentEventFlow.emit(PaymentProcessingEvent.CARD_REACH_OR_INSERT)
        
        // Then
        assertEquals("Please insert or tap your card", viewModel.uiState.value.statusMessage)
        assertTrue(viewModel.uiState.value.showCardAnimation)
    }
}
```

### Integration Tests
```kotlin
class PaymentIntegrationTest {
    
    @Test
    fun `complete payment flow should work end to end`() = runTest {
        // Given
        val paymentQueue = createTestPaymentQueue()
        val paymentItem = createTestPaymentItem()
        
        // When
        paymentQueue.enqueue(paymentItem)
        
        // Then
        paymentQueue.processingState.test {
            assertEquals(ProcessingState.ItemProcessing::class, awaitItem()::class)
            assertEquals(ProcessingState.ItemDone::class, awaitItem()::class)
        }
    }
}
```

This comprehensive example demonstrates how to implement a complete payment processing system using the queue management framework with proper state management, event handling, and user interaction support.
