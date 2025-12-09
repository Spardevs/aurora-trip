package br.com.ticpass.pos.core.queue.input

/**
 * Input Response
 * Represents a response to an input request
 */
data class UserInputResponse(
    val requestId: String,
    val value: Any?,
) {
    companion object {
        /**
         * Create a canceled response
         */
        fun canceled(requestId: String): UserInputResponse {
            return UserInputResponse(requestId, null)
        }
        
        /**
         * Create a timeout response
         */
        fun timeout(requestId: String): UserInputResponse {
            return UserInputResponse(requestId, null)
        }
    }
}
