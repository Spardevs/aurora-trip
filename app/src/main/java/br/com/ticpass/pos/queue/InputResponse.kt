package br.com.ticpass.pos.queue

/**
 * Input Response
 * Represents a response to an input request
 */
data class InputResponse(
    val requestId: String,
    val value: Any?,
) {
    companion object {
        /**
         * Create a canceled response
         */
        fun canceled(requestId: String): InputResponse {
            return InputResponse(requestId, null)
        }
        
        /**
         * Create a timeout response
         */
        fun timeout(requestId: String): InputResponse {
            return InputResponse(requestId, null)
        }
    }
}
