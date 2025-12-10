package br.com.ticpass.pos.core.queue.processors.refund

/**
 * Refund action codes (NO-OP)
 * 
 * This variant does not support refund operations.
 * Minimal stub for compilation compatibility.
 */
enum class AcquirerRefundActionCode(val code: Int) {
    UNSUPPORTED(-1);
}
