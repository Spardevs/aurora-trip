# Input Handling Guide

Comprehensive guide for handling user input requests in the queue system.

## Overview

The queue system supports two types of input requests:

1. **Queue-Level Input Requests**: Handled by `HybridQueueManager` for queue operations
2. **User Input Requests**: Handled by processors for domain-specific input

## Queue-Level Input Requests

### QueueInputRequest Types

```kotlin
sealed class QueueInputRequest {
    data class CONFIRM_NEXT_PROCESSOR(
        override val id: String,
        override val timeoutMs: Long?,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItemId: String,
        val nextItemId: String?
    ) : QueueInputRequest()
    
    data class ERROR_RETRY_OR_SKIP(
        override val id: String,
        override val timeoutMs: Long?,
        val itemId: String,
        val error: ProcessingErrorEvent
    ) : QueueInputRequest()
}
```

### QueueInputResponse Methods

```kotlin
// Confirmation responses
QueueInputResponse.proceed(requestId)     // Continue processing
QueueInputResponse.skip(requestId)        // Skip current item

// Error handling responses
QueueInputResponse.onErrorRetry(requestId)    // Retry immediately
QueueInputResponse.onErrorSkip(requestId)     // Move to end of queue
QueueInputResponse.onErrorAbort(requestId)    // Abort current item
QueueInputResponse.onErrorAbortAll(requestId) // Cancel entire queue

// General responses
QueueInputResponse.cancelled(requestId)   // Request was cancelled
```

## User Input Requests

User input requests are domain-specific and defined by each processor implementation.

### Example Implementation

```kotlin
// Define your user input requests
sealed class UserInputRequest {
    abstract val id: String
    abstract val timeoutMs: Long
    
    data class CONFIRM_RECEIPT_PRINTING(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long = 10_000L
    ) : UserInputRequest()
}

// In your processor
class MyProcessor : QueueProcessor<MyQueueItem, MyEvent> {
    private val _userInputRequests = MutableSharedFlow<UserInputRequest>()
    override val userInputRequests = _userInputRequests.asSharedFlow()
    
    private val _userInputResponses = MutableSharedFlow<UserInputResponse>()
    
    override suspend fun provideUserInput(response: UserInputResponse) {
        _userInputResponses.emit(response)
    }
    
    protected suspend fun requestUserInput(request: UserInputRequest): UserInputResponse {
        _userInputRequests.emit(request)
        
        return withTimeoutOrNull(request.timeoutMs) {
            _userInputResponses.first { it.requestId == request.id }
        } ?: UserInputResponse(request.id, null, false, true) // timeout response
    }
}
```

## Best Practices

1. **Always handle timeouts**: Provide fallback behavior when user doesn't respond
2. **Use appropriate timeout values**: Balance user experience with system responsiveness
3. **Provide clear UI feedback**: Show users what input is expected
4. **Handle cancellation gracefully**: Allow users to cancel input requests
5. **Log input flows**: Add logging for debugging input request/response flows

## Error Handling

Input requests can fail in several ways:

- **Timeout**: User doesn't respond within the specified time
- **Cancellation**: User explicitly cancels the request
- **System Error**: Technical failure during input handling

Always check the response status:

```kotlin
val response = requestUserInput(myRequest)

when {
    response.isCanceled -> {
        // Handle cancellation
        return ProcessingResult.Error(ProcessingErrorEvent.USER_CANCELED)
    }
    response.isTimeout -> {
        // Handle timeout
        return ProcessingResult.Error(ProcessingErrorEvent.TIMEOUT)
    }
    else -> {
        // Use the response value
        val userValue = response.value
        // Continue processing...
    }
}
```
