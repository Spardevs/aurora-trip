package br.com.ticpass.pos.queue.printing

import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.QueueProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Print Processor
 * Handles the processing of print queue items with event emission
 */
class PrintProcessor : QueueProcessor<PrintQueueItem, PrintingEvent> {
    // Event flow implementation
    private val _events = MutableSharedFlow<PrintingEvent>()
    override val events: SharedFlow<PrintingEvent> = _events.asSharedFlow()
    override val inputRequests: SharedFlow<InputRequest>
        get() = TODO("Not yet implemented")

    override suspend fun provideInput(response: InputResponse) {
        TODO("Not yet implemented")
    }

    override suspend fun process(item: PrintQueueItem): ProcessingResult {
        return try {
            // Emit start event
            _events.emit(PrintingEvent.Started(item.id))
            
            // Simulate connecting to printer
            _events.emit(PrintingEvent.ConnectingToPrinter(
                item.id, 
                item.printerId ?: "default"
            ))
            delay(1000)
            
            // Simulate printer connected
            _events.emit(PrintingEvent.PrinterConnected(
                item.id,
                "POS Printer",
                "TP-3000"
            ))
            delay(500)
            
            // Calculate number of pages based on content length and paper size
            val contentLength = item.content.length
            val pageSize = when (item.paperSize) {
                PrintQueueItem.PaperSize.RECEIPT -> 500  // Characters per receipt
                PrintQueueItem.PaperSize.A4 -> 3000      // Characters per A4 page
                PrintQueueItem.PaperSize.LETTER -> 2500  // Characters per Letter page
            }
            val pages = maxOf(1, contentLength / pageSize)
            
            // Simulate sending job to printer
            _events.emit(PrintingEvent.PrintJobSent(
                item.id,
                pages,
                item.copies
            ))
            delay(500)
            
            // Calculate total pages including copies
            val totalPages = pages * item.copies
            
            // Simulate printing with progress updates
            val startTime = System.currentTimeMillis()
            var pagesCompleted = 0
            
            // Random chance of paper running out
            if (Math.random() < 0.2) {
                delay(1500)
                _events.emit(PrintingEvent.OutOfPaper(item.id))
                delay(3000) // Wait for paper refill
            }
            
            // Print progress simulation
            while (pagesCompleted < totalPages) {
                // Print next page
                delay(500) // Time per page
                pagesCompleted++
                
                // Calculate progress percentage
                val percentComplete = (pagesCompleted * 100) / totalPages
                
                // Emit progress update
                _events.emit(PrintingEvent.Progress(
                    item.id,
                    percentComplete,
                    pagesCompleted,
                    totalPages
                ))
            }
            
            // Calculate elapsed time
            val timeElapsed = System.currentTimeMillis() - startTime
            
            // Emit completion event
            val printJobId = UUID.randomUUID().toString()
            _events.emit(PrintingEvent.Completed(
                item.id,
                printJobId,
                timeElapsed
            ))
            
            ProcessingResult.Success
        } catch (e: Exception) {
            // Emit failure event
            _events.emit(PrintingEvent.Failed(
                item.id,
                e.message ?: "Unknown error occurred during printing"
            ))
            ProcessingResult.Error(e.message ?: "Unknown error")
        }
    }
}
