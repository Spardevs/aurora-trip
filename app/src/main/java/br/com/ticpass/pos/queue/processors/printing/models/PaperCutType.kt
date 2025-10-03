package br.com.ticpass.pos.queue.processors.printing.models

/**
 * Paper Cut Type
 * Represents the type of paper cut to perform after printing
 */
enum class PaperCutType {
    /**
     * Full paper cut - completely cuts the paper
     */
    FULL,
    
    /**
     * Partial paper cut - partially cuts the paper (perforated)
     */
    PARTIAL,
    
    /**
     * No paper cut - leaves the paper uncut
     */
    NONE
}
