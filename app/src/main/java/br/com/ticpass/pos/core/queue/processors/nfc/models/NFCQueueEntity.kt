package br.com.ticpass.pos.core.queue.processors.nfc.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.core.queue.processors.nfc.processors.models.NFCProcessorType

/**
 * NFC Entity
 * Room database entity for storing nfc queue items with operation-specific data
 */
@Entity(tableName = "nfc_queue")
data class NFCQueueEntity(
    @PrimaryKey
    val id: String,
    val priority: Int,
    val status: String,
    val processorType: NFCProcessorType,

    // Format operation fields
    val bruteForce: NFCBruteForce? = null,
    
    // Common operation fields
    val timeout: Long? = null,
)
