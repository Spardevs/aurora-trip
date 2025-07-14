package br.com.ticpass.pos.queue

/**
 * Input Response
 * Represents a response to an input request
 */
data class InputResponse(
    val requestId: String,
    val itemId: String,
    val value: Any?, // Could be String for PIN, ByteArray for signature, Boolean for confirmation, Int for selection index
    val canceled: Boolean = false
) {
    companion object {
        /**
         * Create a canceled response
         */
        fun canceled(requestId: String, itemId: String): InputResponse {
            return InputResponse(requestId, itemId, null, true)
        }
        
        /**
         * Create a timeout response
         */
        fun timeout(requestId: String, itemId: String): InputResponse {
            return InputResponse(requestId, itemId, null, true)
        }
    }
}
