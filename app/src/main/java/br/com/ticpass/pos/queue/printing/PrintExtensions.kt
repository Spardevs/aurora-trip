package br.com.ticpass.pos.queue.printing

import br.com.ticpass.pos.queue.QueueItemStatus
import org.json.JSONObject

/**
 * Extension functions for converting between PrintQueueItem and PrintEntity
 */

/**
 * Convert PrintQueueItem to PrintEntity
 */
fun PrintQueueItem.toEntity(status: String = "pending"): PrintEntity {
    return PrintEntity(
        id = this.id,
        timestamp = this.timestamp,
        priority = this.priority,
        content = this.content,
        copies = this.copies,
        paperSize = this.paperSize.name,
        printerId = this.printerId,
        status = status,
        metadata = mapToJson(this.metadata)
    )
}

/**
 * Convert PrintEntity to PrintQueueItem
 */
fun PrintEntity.toQueueItem(): PrintQueueItem {
    return PrintQueueItem(
        id = this.id,
        timestamp = this.timestamp,
        priority = this.priority,
        content = this.content,
        copies = this.copies,
        paperSize = try {
            PrintQueueItem.PaperSize.valueOf(this.paperSize)
        } catch (e: Exception) {
            PrintQueueItem.PaperSize.RECEIPT // Default if parse fails
        },
        printerId = this.printerId,
        metadata = jsonToMap(this.metadata),
        status = QueueItemStatus.valueOf(this.status.uppercase())
    )
}

/**
 * Convert Map to JSON string
 */
private fun mapToJson(map: Map<String, String>): String {
    val json = JSONObject()
    map.forEach { (key, value) -> 
        json.put(key, value)
    }
    return json.toString()
}

/**
 * Convert JSON string to Map
 */
private fun jsonToMap(jsonString: String): Map<String, String> {
    return try {
        val result = mutableMapOf<String, String>()
        val json = JSONObject(jsonString)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.getString(key)
        }
        result
    } catch (e: Exception) {
        emptyMap()
    }
}
