package br.com.ticpass.pos.domain.nfc.usecase

import android.util.Log
import br.com.ticpass.pos.presentation.nfc.states.NFCUiState
import br.com.ticpass.pos.core.queue.core.BaseProcessingEvent
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.input.UserInputResponse
import br.com.ticpass.pos.core.queue.input.QueueInputResponse
import br.com.ticpass.pos.core.queue.core.QueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.presentation.nfc.states.NFCSideEffect
import br.com.ticpass.pos.presentation.nfc.models.NFCTagCustomerDataInput
import br.com.ticpass.pos.presentation.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import javax.inject.Inject

/**
 * Use case for handling processor confirmation operations
 */
class ConfirmationUseCase @Inject constructor() {
    
    /**
     * Confirm proceeding to the next processor
     */
    fun <T : QueueItem, E : BaseProcessingEvent> confirmProcessor(
        requestId: String,
        queue: HybridQueueManager<T, E>,
        modifiedItem: T,
        updateState: (NFCUiState) -> Unit
    ): NFCSideEffect {
        updateState(NFCUiState.Processing)
        Log.d("ConfirmationUseCase", "confirmProcessor called with requestId: $requestId, modifiedItem: $modifiedItem")
        return NFCSideEffect.ProvideQueueInput {
            queue.replaceCurrentItem(modifiedItem)
            queue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Skip the current processor (for confirmation dialogs)
     */
    fun skipProcessor(
        requestId: String,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>
    ): NFCSideEffect {
        return NFCSideEffect.ProvideQueueInput {
            nfcQueue.provideQueueInput(QueueInputResponse.skip(requestId))
        }
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(
        requestId: String,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>
    ): NFCSideEffect {
        return NFCSideEffect.ProvideQueueInput {
            nfcQueue.provideQueueInput(QueueInputResponse.onErrorSkip(requestId))
        }
    }

    /**
     * Confirm NFC keys
     */
    fun confirmNFCKeys(
        requestId: String,
        keys: Map<NFCTagSectorKeyType, String>,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        updateState: (NFCUiState) -> Unit
    ): NFCSideEffect {
        updateState(NFCUiState.Processing)
        // Create an input response with the NFC keys as the value
        val response = UserInputResponse(requestId, keys)
        // Provide input directly to the processor instead of using queue input
        return NFCSideEffect.ProvideProcessorInput {
            nfcQueue.processor.provideUserInput(response)
        }
    }

    /**
     * Confirm NFC keys
     */
    fun confirmNFCTagAuth(
        requestId: String,
        didAuth: Boolean,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        updateState: (NFCUiState) -> Unit
    ): NFCSideEffect {
        updateState(NFCUiState.Processing)
        // Create an input response with the NFC keys as the value
        val response = UserInputResponse(requestId, didAuth)
        // Provide input directly to the processor instead of using queue input
        return NFCSideEffect.ProvideProcessorInput {
            nfcQueue.processor.provideUserInput(response)
        }
    }

    /**
     * Confirm NFC customer data
     */
    fun confirmNFCustomerData(
        requestId: String,
        data: NFCTagCustomerDataInput?,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        updateState: (NFCUiState) -> Unit
    ): NFCSideEffect {
        updateState(NFCUiState.Processing)
        // Create an input response with the customer data as the value
        val response = UserInputResponse(requestId, data)
        // Provide input directly to the processor instead of using queue input
        return NFCSideEffect.ProvideProcessorInput {
            nfcQueue.processor.provideUserInput(response)
        }
    }

    fun confirmNFCCustomerSavePin(
        requestId: String,
        didSave: Boolean,
        nfcQueue: HybridQueueManager<NFCQueueItem, NFCEvent>,
        updateState: (NFCUiState) -> Unit
    ): NFCSideEffect {
        updateState(NFCUiState.Processing)
        val response = UserInputResponse(requestId, didSave)
        // Provide input directly to the processor instead of using queue input
        return NFCSideEffect.ProvideProcessorInput {
            nfcQueue.processor.provideUserInput(response)
        }
    }
}
