package br.com.ticpass.pos.queue.printing

import br.com.ticpass.pos.queue.BaseProcessingEvent

/**
 * Print-specific events emitted during print processing
 */
sealed class PrintingEvent : BaseProcessingEvent {
    /**
     * Emitted when print job starts
     */
    data class Started(val printId: String) : PrintingEvent()
    
    /**
     * Emitted when printer is connecting
     */
    data class ConnectingToPrinter(
        val printId: String,
        val printerId: String?
    ) : PrintingEvent()
    
    /**
     * Emitted when printer is connected
     */
    data class PrinterConnected(
        val printId: String,
        val printerName: String,
        val printerModel: String
    ) : PrintingEvent()
    
    /**
     * Emitted when print job is sent to printer
     */
    data class PrintJobSent(
        val printId: String,
        val pages: Int,
        val copies: Int
    ) : PrintingEvent()
    
    /**
     * Emitted with print progress updates
     */
    data class Progress(
        val printId: String,
        val percentComplete: Int,
        val pagesCompleted: Int,
        val totalPages: Int
    ) : PrintingEvent()
    
    /**
     * Emitted when out of paper
     */
    data class OutOfPaper(val printId: String) : PrintingEvent()
    
    /**
     * Emitted when print job is completed successfully
     */
    data class Completed(
        val printId: String,
        val printJobId: String,
        val timeElapsed: Long
    ) : PrintingEvent()
    
    /**
     * Emitted when print job fails
     */
    data class Failed(
        val printId: String,
        val error: String
    ) : PrintingEvent()
}
